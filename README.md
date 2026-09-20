# 📰 Diaryomi

O **Diaryomi** é um aplicativo Android moderno e de código aberto desenvolvido para automatizar o monitoramento de termos, nomes e publicações em **Diários Oficiais**. 

Com o Diaryomi, você cadastra os termos de seu interesse (como seu nome, CPF, razão social ou palavras-chave de licitações e concursos) e o aplicativo consulta periodicamente as edições oficiais, notificando você sempre que uma nova matéria for encontrada.

---

## 📌 Sumário
- [Guia do Usuário](#-guia-do-usuário)
  - [Principais Funcionalidades](#principais-funcionalidades)
  - [Diários Disponíveis](#diários-disponíveis)
  - [Como Usar](#como-usar)
- [Guia do Desenvolvedor](#-guia-do-desenvolvedor)
  - [Tecnologias Utilizadas](#tecnologias-utilizadas)
  - [Arquitetura do Projeto](#arquitetura-do-projeto)
  - [Estrutura de Diretórios](#estrutura-de-diretórios)
  - [Como Adicionar um Novo Diário](#como-adicionar-um-novo-diário-extensão)
  - [Compilação e Testes](#compilação-e-testes)
  - [Diretrizes de Contribuição](#diretrizes-de-contribuição)

---

## 📱 Guia do Usuário

### Principais Funcionalidades

- 🔍 **Monitoramento Inteligente**: Defina termos de busca exatos ou compostos e acompanhe matérias a partir de uma data inicial definida.
- ⏰ **Sincronização em Segundo Plano**: O aplicativo consulta automaticamente os diários a cada 24 horas via rotina em background.
- 🔔 **Notificações Sem Repetição**: O app possui deduplicação avançada. Consultar no mesmo dia ou posteriormente não repete notificações para matérias já encontradas.
- 📅 **Consulta Antecipada**: As buscas cobrem sempre o dia atual e o dia seguinte, permitindo que você visualize edições lançadas antecipadamente (comum em diários eletrônicos no fim do dia).
- ✔️ **Gestão de Leitura Flexível**:
  - **Individual**: Marque uma publicação como lida diretamente pelo cartão, sem a necessidade de abri-la.
  - **Em Lote**: Pressione e segure qualquer publicação para entrar no modo de seleção múltipla, marque quantas quiser e confirme a leitura em um toque.
  - **Marcar Todas**: Ação rápida para marcar todas as publicações como lidas na tela de detalhes ou na listagem principal.
- 🔗 **Links Sempre Válidos (TCE-RN)**: Resolve automaticamente os links temporários de PDF a cada clique, garantindo que documentos protegidos por tokens temporários abram sem erro de expiração.
- 📜 **Console de Logs**: Visualize em tempo real as requisições HTTP, parâmetros de consulta e status de cada sincronização.

### Diários Disponíveis

1. **Governo Federal (DOU - Diário Oficial da União)**: Pesquisa artigos e atos do Poder Executivo Federal via consulta web otimizada.
2. **TCE-RN (Tribunal de Contas do Estado do Rio Grande do Norte)**: Pesquisa edições completas e atos normativos individuais na API SisDocs oficial do tribunal.

---

### Como Usar

#### 1. Criando uma Busca Monitorada
1. Abra o aplicativo e toque no botão **"+ Nova busca"** no canto inferior direito.
2. Selecione o **Diário Oficial** desejado (ex.: *Governo Federal (DOU)* ou *TCE-RN*).
3. Insira os termos que deseja monitorar (ex.: `João da Silva`, `Portaria 123`).
4. Escolha a **Data Inicial** retroativa a partir da qual o sistema deve buscar os diários.
5. Toque em **"Salvar e Iniciar Busca"**. O app fará a primeira consulta imediatamente.

#### 2. Visualizando Resultados
- Na tela inicial, cada cartão exibe o diário, os termos pesquisados, a data de início e a quantidade de publicações não lidas.
- Toque no cartão para abrir a listagem detalhada de matérias agrupadas por data de publicação.
- Cada matéria exibe um resumo (*snippet*) com as palavras-chave destacadas.
- Puxe a tela para baixo (*Pull-to-Refresh*) para forçar uma nova consulta manual a qualquer momento.

#### 3. Marcando Matérias como Lidas
- **Sem abrir o link**: Toque no botão **"Marcar lida"** no rodapé do card da matéria.
- **Várias matérias ao mesmo tempo**:
  1. Pressione e segure (*long press*) qualquer card ou toque no ícone de checklist no topo da tela.
  2. Marque as publicações desejadas (ou toque em *Selecionar todas* no canto superior).
  3. Toque no ícone de confirmação no topo da tela.
- **Todas de uma vez**: Toque no menu de três pontos da tela de detalhes ou no menu do cartão na tela inicial e selecione **"Marcar todas como lidas"**.

#### 4. Acessando os Documentos Oficiais
- Toque no corpo do cartão ou no ícone de link externo para abrir a matéria original no navegador ou baixar o PDF oficial.
- No módulo TCE-RN, o app faz uma requisição instantânea aos servidores do tribunal para renovar o link temporário do PDF, garantindo que o arquivo abra sempre atualizado.

---

## 💻 Guia do Desenvolvedor

Esta seção é destinada a quem deseja entender o funcionamento interno do Diaryomi, compilar o código-fonte ou contribuir com novas funcionalidades e diários.

### Tecnologias Utilizadas

- **Linguagem**: [Kotlin 2.1](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) com [Material Design 3](https://m3.material.io/)
- **Injeção de Dependências**: [Hilt / Dagger](https://dagger.dev/hilt/)
- **Banco de Dados Local**: [Room (SQLite)](https://developer.android.com/training/data-storage/room)
- **Tarefas em Segundo Plano**: [AndroidX WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- **Rede & Parsing**:
  - [OkHttp 4](https://square.github.io/okhttp/) (com suporte a cookies persistentes em memória e redirects)
  - [Jsoup](https://jsoup.org/) (extração de HTML/scripts estruturados)
  - [Gson](https://github.com/google/gson) (serialização e deserialização JSON)
- **Assincronia**: Kotlin Coroutines & Flow

---

### Arquitetura do Projeto

O Diaryomi segue os princípios da **Clean Architecture** combinados com o padrão de projeto **Strategy Pattern** para suportar a adição simples e desacoplada de novos diários:

```
┌─────────────────────────────────────────────────────────┐
│                   Apresentação (UI)                     │
│         Jetpack Compose, ViewModels, StateFlow          │
└────────────────────────────┬────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────┐
│                    Domínio (Domain)                     │
│         UseCases, Modelos de Negócio, Repositórios      │
└────────────────────────────┬────────────────────────────┘
                             │
       ┌─────────────────────┴─────────────────────┐
       ▼                                           ▼
┌─────────────────────────────┐     ┌─────────────────────────────┐
│       Dados (Data)          │     │    Extensões (Extensions)   │
│ Room, DAOs, Entidades, Impl │     │ DOU, TCE-RN, AMRN, etc.    │
└─────────────────────────────┘     └─────────────────────────────┘
```

---

### Estrutura de Diretórios

```
app/src/main/java/com/diaryomi/
├── data/                         # Camada de Dados
│   ├── local/                    # Room Database, DAOs, Entidades e Conversores
│   └── repository/               # Implementação do repositório (com deduplicação inteligente)
├── di/                           # Módulos de injeção de dependência do Hilt
├── domain/                       # Camada de Domínio
│   ├── model/                    # Modelos de dados de domínio (TrackedSearch, SearchResult, GazetteResult)
│   ├── repository/               # Contratos de repositório
│   └── usecase/                  # Casos de uso (SyncUseCase, MarkResultReadUseCase, etc.)
├── extension/                    # Módulos de Diários Oficiais (Strategy Pattern)
│   ├── GazetteExtension.kt       # Interface base de qualquer diário oficial
│   ├── GazetteExtensionRegistry.kt # Registro central com injeção automática
│   ├── govfederal/               # Extensão do Diário Oficial da União (DOU)
│   └── tcern/                    # Extensão do TCE-RN (SisDocs API + PDF resolver)
├── ui/                           # Camada de Apresentação
│   ├── detail/                   # Tela de detalhes, seleção múltipla e listagem de resultados
│   ├── library/                  # Tela principal (biblioteca de buscas monitoradas)
│   ├── navigation/               # Rotas e grafo de navegação Compose
│   └── theme/                    # Tipografia, cores e tema Material 3
├── util/                         # Utilitários (AppLogger em tempo real, TextNormalizer)
└── worker/                       # DailySyncWorker (agendamento periódico WorkManager)
```

---

### Como Adicionar um Novo Diário (Extensão)

Adicionar um novo diário é extremamente simples graças ao **Strategy Pattern** e ao **Hilt Multibindings**.

#### 1. Implemente a interface `GazetteExtension`
Crie um pacote em `com.diaryomi.extension.<nome_do_diario>` e implemente a interface:

```kotlin
package com.diaryomi.extension.meudiario

import com.diaryomi.domain.model.GazetteResult
import com.diaryomi.extension.GazetteExtension
import java.time.LocalDate
import javax.inject.Inject

class MeuDiarioExtension @Inject constructor(
    private val httpClient: OkHttpClient
) : GazetteExtension {

    override val id: String = "meu_diario"
    override val name: String = "Diário Oficial do Meu Estado"

    override suspend fun search(
        terms: List<String>,
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<GazetteResult> {
        // 1. Faça a requisição HTTP para a API ou página de busca do diário
        // 2. Extraia as matérias correspondentes aos termos fornecidos
        // 3. Retorne uma lista de GazetteResult
        return listOf(
            GazetteResult(
                title = "Edição nº 100 - Portaria 45",
                date = LocalDate.now(),
                snippet = "Trecho da publicação contendo os termos...",
                url = "https://meudiario.gov.br/download/100.pdf",
                matchedTerms = terms
            )
        )
    }

    // Opcional: Sobrescreva se o diário utilizar links temporários que precisem
    // ser gerados sob demanda no momento do clique (como no caso do TCE-RN):
    override suspend fun resolveUrl(url: String): String {
        return url
    }
}
```

#### 2. Registre no Módulo Hilt (`AppModule.kt`)
Em `com.diaryomi.di.AppModule.kt`, no `ExtensionModule`, adicione o binding da nova extensão:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class ExtensionModule {
    @Binds
    @IntoSet
    abstract fun bindMeuDiarioExtension(impl: MeuDiarioExtension): GazetteExtension
}
```

**Pronto!** O novo diário aparecerá automaticamente no modal de seleção de diários da tela inicial e participará do ciclo de sincronização em segundo plano.

---

### Compilação e Testes

#### Pré-requisitos
- **JDK 17** ou superior instalado e configurado no `PATH`.
- **Android SDK** com suporte à API 35 instalado (geralmente gerenciado pelo Android Studio).

#### Comandos Gradle

```bash
# Compilar o código Kotlin
./gradlew compileDebugKotlin

# Executar a suíte de testes unitários
./gradlew testDebugUnitTest

# Gerar o pacote APK de depuração
./gradlew assembleDebug
```
*O arquivo gerado estará disponível em `app/build/outputs/apk/debug/app-debug.apk`.*

---

### Diretrizes de Contribuição

Contribuições são muito bem-vindas! Siga estas etapas para contribuir:

1. **Faça um Fork** do repositório.
2. **Crie uma branch** para sua funcionalidade ou correção:
   ```bash
   git checkout -b feature/sua-feature
   ```
3. **Escreva código limpo** seguindo as convenções do Kotlin e os princípios de Clean Architecture adotados no projeto.
4. **Adicione testes unitários** para cobrir a nova lógica em `app/src/test/java/`.
5. **Certifique-se de que o projeto compila e os testes passam**:
   ```bash
   ./gradlew testDebugUnitTest
   ```
6. **Abra um Pull Request** detalhando as motivações da alteração e testes realizados.

---

## ⚖️ Licença

Este projeto é disponibilizado sob a licença [MIT](LICENSE). Sinta-se livre para usar, modificar e distribuir de acordo com os termos da licença.
