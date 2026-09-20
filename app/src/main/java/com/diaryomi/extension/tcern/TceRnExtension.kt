package com.diaryomi.extension.tcern

import com.diaryomi.domain.model.GazetteResult
import com.diaryomi.extension.GazetteExtension
import com.diaryomi.util.AppLogger
import com.diaryomi.util.TextNormalizer
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Extensão para busca no Diário Eletrônico do TCE-RN.
 *
 * Fluxo de dados da API SisDocs:
 * 1. Faz GET em https://sisdocs.tce.rn.gov.br/api/v1/DiarioEletronico?dataInicio=...&dataFim=...
 * 2. Inspeciona cada publicação no array `diarioEletronicoPublicacaos` e seu `conteudoOriginal`.
 * 3. Identifica `idDocumentoOficial` (ex: 312).
 * 4. Resolve o link direto do PDF fazendo GET em:
 *    https://sisdocs.tce.rn.gov.br/api/v1/DiarioEletronico/download/{idDocumentoOficial}
 *    que retorna a URL direta temporária do PDF para visualização/download.
 */
class TceRnExtension @Inject constructor(
    private val httpClient: OkHttpClient,
    private val gson: Gson
) : GazetteExtension {

    override val id: String = "tce_rn"
    override val name: String = "TCE-RN"

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val BASE_URL = "https://sisdocs.tce.rn.gov.br/api/v1/DiarioEletronico"

    override suspend fun search(
        terms: List<String>,
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<GazetteResult> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL?dataInicio=${fromDate.format(dateFormatter)}" +
                "&dataFim=${toDate.format(dateFormatter)}" +
                "&publicado=true" +
                "&verificaDataExclusao=true"

        AppLogger.i("[TCE-RN] Consultando diários do período: ${fromDate.format(dateFormatter)} a ${toDate.format(dateFormatter)}")
        AppLogger.d("[TCE-RN] GET $url")

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            AppLogger.d("[TCE-RN] Resposta HTTP: ${response.code} ${response.message}")

            if (!response.isSuccessful) {
                AppLogger.w("[TCE-RN] Servidor retornou HTTP ${response.code}")
                return@withContext emptyList()
            }

            val jsonStr = response.body?.string() ?: run {
                AppLogger.w("[TCE-RN] Corpo da resposta vazio.")
                return@withContext emptyList()
            }

            val results = parseAndFilter(jsonStr, terms)
            AppLogger.i("[TCE-RN] Concluído: ${results.size} matéria(s) encontrada(s) no período.")
            results
        } catch (e: Exception) {
            AppLogger.e("[TCE-RN] Erro na requisição HTTP", e)
            emptyList()
        }
    }

    /**
     * Parseia a resposta JSON da API do TCE-RN e filtra por publicações contendo todos os termos.
     */
    private fun parseAndFilter(jsonStr: String, terms: List<String>): List<GazetteResult> {
        return try {
            val jsonArray = gson.fromJson(jsonStr, JsonArray::class.java)
            AppLogger.d("[TCE-RN] Total de edições baixadas no período: ${jsonArray.size()}")

            val results = mutableListOf<GazetteResult>()

            for (element in jsonArray) {
                val diario = element.asJsonObject

                val idDiario = diario.get("idDiarioEletronico")?.asLong
                    ?: diario.get("id")?.asLong
                    ?: 0L

                val docOficial = diario.getAsJsonObject("documentoOficial")

                // idDocumentoOficial utilizado para obter o link direto do PDF
                val idDocOficial = docOficial?.get("idDocumentoOficial")?.asLong
                    ?: diario.get("idDocumentoOficial")?.asLong
                    ?: 0L

                val numeroDiario = docOficial?.get("numeroDocumento")?.asString
                    ?: diario.get("numero")?.asString
                    ?: diario.get("numeroDiario")?.asString
                    ?: idDiario.toString()

                // Data de publicação do diário
                val diarioDateStr = docOficial?.get("dataPublicacao")?.asString
                    ?: diario.get("dataPublicacao")?.asString
                    ?: diario.get("dataInclusao")?.asString
                    ?: ""
                val defaultPubDate = parseDate(diarioDateStr)

                val publicacoes = diario.getAsJsonArray("diarioEletronicoPublicacaos")

                if (publicacoes != null && publicacoes.size() > 0) {
                    var matchesInThisDiario = 0

                    for (pubElem in publicacoes) {
                        val pub = pubElem.asJsonObject
                        val conteudo = pub.get("conteudoOriginal")?.asString ?: ""
                        val numDiarioPub = pub.get("numeroDiario")?.asString ?: numeroDiario
                        val pubId = pub.get("idPublicacao")?.asString ?: ""

                        // Verifica se a matéria contém algum dos termos (insensível a quebras de linha/espaços/acentos)
                        val foundTerms = TextNormalizer.findMatchingTerms(conteudo, terms)
                        if (foundTerms.isNotEmpty()) {
                            val pubDateStr = pub.get("dataEdicao")?.asString
                                ?: pub.get("dataInclusao")?.asString
                                ?: diarioDateStr
                            val pubDate = parseDate(pubDateStr, defaultPubDate)

                            val actTitle = extractActTitle(conteudo)
                            val title = if (!actTitle.isNullOrBlank()) {
                                "Diário TCE-RN nº $numDiarioPub • $actTitle"
                            } else {
                                "Diário TCE-RN nº $numDiarioPub (Publicação #$pubId)"
                            }

                            val snippet = extractSnippet(conteudo, terms)

                            // Salva a URL persistente do endpoint de download com fragmento único da publicação
                            val stablePdfUrl = if (idDocOficial > 0) {
                                if (pubId.isNotBlank()) {
                                    "$BASE_URL/download/$idDocOficial#pub-$pubId"
                                } else {
                                    "$BASE_URL/download/$idDocOficial"
                                }
                            } else if (idDiario > 0) {
                                "$BASE_URL/$idDiario/pdf"
                            } else {
                                diario.get("url")?.asString ?: "https://sisdocs.tce.rn.gov.br"
                            }

                            AppLogger.i("🎯 [TCE-RN] Matéria encontrada na publicação #$pubId (Diário nº $numDiarioPub): $title [Termos: ${foundTerms.joinToString()}]")

                            results.add(
                                GazetteResult(
                                    title = title,
                                    date = pubDate,
                                    snippet = snippet,
                                    url = stablePdfUrl,
                                    matchedTerms = foundTerms
                                )
                            )
                            matchesInThisDiario++
                        }
                    }

                    // Se nenhuma matéria individual teve match, mas a edição completa tem:
                    if (matchesInThisDiario == 0) {
                        val fullEditionText = buildString {
                            for (p in publicacoes) {
                                append(p.asJsonObject.get("conteudoOriginal")?.asString ?: "")
                                append(" ")
                            }
                        }

                        val editionFoundTerms = TextNormalizer.findMatchingTerms(fullEditionText, terms)
                        if (editionFoundTerms.isNotEmpty()) {
                            AppLogger.i("🎯 [TCE-RN] Correspondência encontrada no conjunto da edição do Diário nº $numeroDiario [Termos: ${editionFoundTerms.joinToString()}]")
                            val stablePdfUrl = if (idDocOficial > 0) {
                                "$BASE_URL/download/$idDocOficial#edition-$numeroDiario"
                            } else if (idDiario > 0) {
                                "$BASE_URL/$idDiario/pdf"
                            } else {
                                "https://sisdocs.tce.rn.gov.br"
                            }

                            results.add(
                                GazetteResult(
                                    title = "Diário Eletrônico TCE-RN nº $numeroDiario",
                                    date = defaultPubDate,
                                    snippet = extractSnippet(fullEditionText, terms),
                                    url = stablePdfUrl,
                                    matchedTerms = editionFoundTerms
                                )
                            )
                        }
                    }
                } else {
                    // Fallback para modelos legados/planos
                    val numero = diario.get("numero")?.asString ?: numeroDiario
                    val descricao = diario.get("descricao")?.asString ?: ""
                    val observacao = diario.get("observacao")?.asString ?: ""
                    val fullText = "$numero $descricao $observacao"

                    val legacyFoundTerms = TextNormalizer.findMatchingTerms(fullText, terms)
                    if (legacyFoundTerms.isNotEmpty()) {
                        val stablePdfUrl = if (idDocOficial > 0) {
                            "$BASE_URL/download/$idDocOficial#legacy-$numero"
                        } else if (idDiario > 0) {
                            "$BASE_URL/$idDiario/pdf"
                        } else {
                            "https://sisdocs.tce.rn.gov.br"
                        }
                        results.add(
                            GazetteResult(
                                title = "Diário Eletrônico TCE-RN nº $numero",
                                date = defaultPubDate,
                                snippet = extractSnippet(fullText, terms),
                                url = stablePdfUrl,
                                matchedTerms = legacyFoundTerms
                            )
                        )
                    }
                }
            }

            results
        } catch (e: Exception) {
            AppLogger.e("[TCE-RN] Falha ao processar e filtrar JSON", e)
            emptyList()
        }
    }

    /**
     * Resolve a URL final do PDF a cada clique.
     * Como o link direto que o TCE-RN oferta é temporário (expira em novaarearestrita.tce.rn.gov.br),
     * geramos uma nova URL direta no endpoint /download/{idDocumentoOficial} a cada clique do usuário.
     */
    override suspend fun resolveUrl(url: String): String = withContext(Dispatchers.IO) {
        if (url.contains("/DiarioEletronico/download/")) {
            val cleanUrl = url.substringBefore("#")
            return@withContext fetchFreshPdfUrl(cleanUrl)
        }
        url
    }

    /**
     * Faz GET no endpoint /download/{idDocumentoOficial} para obter um novo link temporário válido.
     */
    private fun fetchFreshPdfUrl(downloadEndpoint: String): String {
        AppLogger.d("[TCE-RN] Gerando novo link temporário de PDF via $downloadEndpoint...")

        return try {
            val request = Request.Builder()
                .url(downloadEndpoint)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()

            val response = httpClient.newCall(request).execute()
            val rawBody = response.body?.string()?.trim()?.replace("\"", "") ?: ""

            if (response.isSuccessful && rawBody.startsWith("http")) {
                AppLogger.i("📄 [TCE-RN] Novo link direto do PDF gerado com sucesso: $rawBody")
                rawBody
            } else {
                AppLogger.w("[TCE-RN] Endpoint download retornou HTTP ${response.code}: $rawBody")
                downloadEndpoint
            }
        } catch (e: Exception) {
            AppLogger.e("[TCE-RN] Falha ao consultar endpoint de download: ${e.message}", e)
            downloadEndpoint
        }
    }

    /**
     * Extrai a data no formato ISO YYYY-MM-DD
     */
    private fun parseDate(dateStr: String, fallback: LocalDate = LocalDate.now()): LocalDate {
        return try {
            if (dateStr.length >= 10) {
                LocalDate.parse(dateStr.substring(0, 10))
            } else {
                fallback
            }
        } catch (e: Exception) {
            fallback
        }
    }

    /**
     * Extrai o título do ato normativo ou administrativo a partir do conteúdo original
     */
    private fun extractActTitle(text: String): String? {
        val lines = text.split("\n", "\r")
            .map { it.trim().replace("\\s+".toRegex(), " ") }
            .filter { it.isNotBlank() }

        for (line in lines.take(12)) {
            val upper = line.uppercase()
            if (upper.startsWith("PORTARIA") ||
                upper.startsWith("DECISÃO") || upper.startsWith("DECISAO") ||
                upper.startsWith("EDITAL") ||
                upper.startsWith("RESOLUÇÃO") || upper.startsWith("RESOLUCAO") ||
                upper.startsWith("PROCESSO") ||
                upper.startsWith("DOCUMENTO") ||
                upper.startsWith("ACÓRDÃO") || upper.startsWith("ACORDAO") ||
                upper.startsWith("DESPACHO") ||
                upper.startsWith("AVISO")
            ) {
                return line.take(80)
            }
        }
        return lines.firstOrNull()?.take(80)
    }

    /**
     * Extrai um trecho inteligente do texto centralizado ao redor de um dos termos pesquisados.
     */
    private fun extractSnippet(text: String, terms: List<String>): String {
        val cleanText = text.replace("\\s+".toRegex(), " ").trim()
        if (cleanText.isEmpty()) return ""

        val normalizedText = TextNormalizer.normalize(cleanText)
        var matchIndex = -1
        var matchedTermLength = 0

        for (term in terms) {
            val normalizedTerm = TextNormalizer.normalize(term)
            if (normalizedTerm.isNotEmpty()) {
                val idx = normalizedText.indexOf(normalizedTerm)
                if (idx != -1) {
                    matchIndex = idx
                    matchedTermLength = normalizedTerm.length
                    break
                }
            }
        }

        if (matchIndex == -1) {
            return cleanText.take(300)
        }

        val start = maxOf(0, matchIndex - 60)
        val end = minOf(cleanText.length, matchIndex + matchedTermLength + 140)

        val prefix = if (start > 0) "… " else ""
        val suffix = if (end < cleanText.length) " …" else ""

        return prefix + cleanText.substring(start, end).trim() + suffix
    }
}
