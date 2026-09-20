package com.diaryomi.extension.govfederal

import com.diaryomi.domain.model.GazetteResult
import com.diaryomi.extension.GazetteExtension
import com.diaryomi.util.AppLogger
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Extensão para busca no Diário Oficial da União (DOU) - Governo Federal.
 *
 * Lógica:
 * - Concatena termos com %22 (aspas para busca exata) e + (espaços)
 * - Faz GET na API da Imprensa Nacional em Dispatchers.IO
 * - Parseia o HTML retornado usando Jsoup
 * - Extrai o JSON do script tag com ID específico
 * - Retorna lista de GazetteResult com logging detalhado
 */
class GovFederalExtension @Inject constructor(
    private val httpClient: OkHttpClient,
    private val gson: Gson
) : GazetteExtension {

    override val id: String = "gov_federal"
    override val name: String = "Governo Federal (DOU)"

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    companion object {
        private const val BASE_URL = "https://www.in.gov.br/consulta/-/buscar/dou"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
        private const val ACCEPT =
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"
        private const val ACCEPT_LANGUAGE = "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7"
    }

    private fun buildBrowserRequest(url: String, referer: String? = null): Request {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", ACCEPT)
            .header("Accept-Language", ACCEPT_LANGUAGE)
            .header("Sec-Ch-Ua", "\"Chromium\";v=\"128\", \"Not;A=Brand\";v=\"24\", \"Google Chrome\";v=\"128\"")
            .header("Sec-Ch-Ua-Mobile", "?0")
            .header("Sec-Ch-Ua-Platform", "\"Windows\"")
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "same-origin")
            .header("Sec-Fetch-User", "?1")
            .header("Upgrade-Insecure-Requests", "1")
        if (referer != null) {
            builder.header("Referer", referer)
        }
        return builder.build()
    }

    override suspend fun search(
        terms: List<String>,
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<GazetteResult> = withContext(Dispatchers.IO) {
        val query = terms.joinToString("+") { term ->
            "%22" + term.trim().replace(" ", "+") + "%22"
        }

        val url = "$BASE_URL?q=$query" +
                "&s=todos" +
                "&exactDate=personalizado" +
                "&sortType=0" +
                "&publishFrom=${fromDate.format(dateFormatter)}" +
                "&publishTo=${toDate.format(dateFormatter)}" +
                "&delta=20"

        AppLogger.i("[DOU] Pesquisando termos: ${terms.joinToString(", ")} | Período: ${fromDate.format(dateFormatter)} a ${toDate.format(dateFormatter)}")
        AppLogger.d("[DOU] GET $url")

        try {
            var response = httpClient.newCall(buildBrowserRequest(url)).execute()
            AppLogger.d("[DOU] Resposta HTTP: ${response.code} ${response.message}")

            if (response.code == 403) {
                AppLogger.w("[DOU] HTTP 403 detectado (WAF Azion). Tentando aquecer sessão em https://www.in.gov.br/...")
                response.close()

                try {
                    val warmupReq = buildBrowserRequest("https://www.in.gov.br/")
                    val warmupRes = httpClient.newCall(warmupReq).execute()
                    AppLogger.d("[DOU] Warmup HTTP: ${warmupRes.code}")
                    warmupRes.close()
                } catch (e: Exception) {
                    AppLogger.w("[DOU] Aviso no warmup: ${e.message}")
                }

                AppLogger.d("[DOU] Repetindo busca com sessão aquecida...")
                response = httpClient.newCall(buildBrowserRequest(url, referer = "https://www.in.gov.br/")).execute()
                AppLogger.d("[DOU] Resposta pós-warmup: ${response.code} ${response.message}")
            }

            if (!response.isSuccessful) {
                val errorPreview = response.body?.string()?.take(250)?.trim() ?: ""
                AppLogger.w("[DOU] Servidor retornou HTTP ${response.code}. Detalhes: $errorPreview")
                return@withContext emptyList()
            }

            val html = response.body?.string() ?: run {
                AppLogger.w("[DOU] Corpo da resposta vazio.")
                return@withContext emptyList()
            }

            val results = parseHtmlResponse(html, terms)
            AppLogger.i("[DOU] Concluído: ${results.size} matéria(s) encontrada(s).")
            results
        } catch (e: Exception) {
            AppLogger.e("[DOU] Erro na requisição HTTP", e)
            emptyList()
        }
    }

    /**
     * Parseia o HTML da página de busca do DOU.
     * Procura o script tag com ID específico que contém os dados de busca em JSON.
     */
    private fun parseHtmlResponse(html: String, terms: List<String>): List<GazetteResult> {
        val document = Jsoup.parse(html)

        val scriptTag = document.selectFirst(
            "script#_br_com_seatecnologia_in_buscadou_BuscaDouPortlet_params"
        )

        if (scriptTag == null) {
            AppLogger.w("[DOU] Tag script com resultados não encontrada no HTML.")
            return emptyList()
        }

        val jsonText = scriptTag.data().trim()
        if (jsonText.isEmpty()) {
            AppLogger.w("[DOU] Conteúdo JSON do script está vazio.")
            return emptyList()
        }

        return try {
            val jsonObject = gson.fromJson(jsonText, JsonObject::class.java)
            val jsonArray = jsonObject.getAsJsonArray("jsonArray")

            if (jsonArray == null || jsonArray.isEmpty) {
                AppLogger.d("[DOU] Nenhum item no array de resultados.")
                return emptyList()
            }

            val results = mutableListOf<GazetteResult>()

            for (element in jsonArray) {
                val item = element.asJsonObject
                val title = item.get("title")?.asString ?: "Sem título"
                val content = item.get("content")?.asString ?: ""
                val urlTitle = item.get("urlTitle")?.asString ?: ""
                val pubDateStr = item.get("pubDate")?.asString
                val articleUrl = if (urlTitle.isNotEmpty()) {
                    "https://www.in.gov.br/web/dou/-/$urlTitle"
                } else ""

                val pubDate = try {
                    if (pubDateStr != null) {
                        LocalDate.parse(pubDateStr, dateFormatter)
                    } else LocalDate.now()
                } catch (e: Exception) {
                    LocalDate.now()
                }

                val snippetText = Jsoup.parse(content).text()

                val foundTerms = terms.filter { term ->
                    com.diaryomi.util.TextNormalizer.containsIgnoreCaseAndAccents("$title $snippetText", term)
                }

                results.add(
                    GazetteResult(
                        title = title,
                        date = pubDate,
                        snippet = snippetText.take(300),
                        url = articleUrl,
                        matchedTerms = if (foundTerms.isNotEmpty()) foundTerms else terms
                    )
                )
            }

            results
        } catch (e: Exception) {
            AppLogger.e("[DOU] Falha ao processar JSON dos resultados", e)
            emptyList()
        }
    }
}
