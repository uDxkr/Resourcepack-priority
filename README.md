# ResourcePack Priorizer

Mod **client-side** para Minecraft Java 1.21.11 (Fabric) que deixa os seus resource packs locais
terem prioridade sobre o resource pack enviado pelo servidor.

O pack do servidor continua **baixado, ativado e intacto**. O mod só muda a ordem de empilhamento
no cliente — não bloqueia, não remove, não falsifica nada e não precisa de mod no servidor.

```
PRIORIDADE ALTA
────────────────────────────────
WFG PACK 2025                [LOCAL]
My Custom Pack               [LOCAL]
Factions ICE                 [SERVER]
redfantasy.com               [SERVER]
Default Minecraft            [BUILTIN]
────────────────────────────────
PRIORIDADE BAIXA
```

- Autor: **dxkr** — <https://github.com/udxkr>
- Pacote da source: `com.github.udxkr`

---

## 1. Como o Minecraft 1.21.11 monta a lista de resource packs

Verificado diretamente nos bytecodes da 1.21.11 (mappings Yarn `1.21.11+build.6`), não de memória.

`ResourcePackManager` (`net.minecraft.resource`) guarda três coisas:

| Campo | Tipo | Papel |
|---|---|---|
| `providers` | `Set<ResourcePackProvider>` | quem descobre packs |
| `profiles` | `Map<String, ResourcePackProfile>` | todos os packs conhecidos |
| `enabled` | `List<ResourcePackProfile>` | **a pilha ativa, em ordem** |

`MinecraftClient` registra os providers no construtor:

- `DefaultClientResourcePackProvider` → pack `vanilla` e built-ins
- `FileResourcePackProvider(resourcepacks/, CLIENT_RESOURCES, ResourcePackSource.NONE, …)` → **seus packs locais**
- `ServerResourcePackLoader#getPassthroughPackProvider()` → **os packs do servidor**

## 2. Onde o pack do servidor entra

`ServerResourcePackLoader` cria os profiles em `toProfiles(...)` com:

```java
// ServerResourcePackLoader, campo estático POSITION
new ResourcePackPosition(true, ResourcePackProfile.InsertionPosition.TOP, true)
//                       ^required                 ^topo                  ^posição travada
```

e com `ResourcePackInfo(id, SERVER_NAME_TEXT, ResourcePackSource.SERVER, …)`, onde o id tem o
formato `server/%08X/<uuid>`.

Ou seja: `required` (não dá para desativar) + `TOP` (prioridade máxima) + `fixedPosition`
(a tela de packs do vanilla se recusa a mover). **É exatamente por isso que você não consegue
colocar o seu pack por cima pela GUI normal.**

## 3. Quem controla a prioridade de verdade

A ordem da lista `enabled`. `createResourcePacks()` preserva essa ordem até
`LifecycledResourceManagerImpl`, e a resolução final acontece em
`NamespaceResourceManager#getResource`, que percorre a lista **de trás para frente**:

```java
for (int i = this.packList.size() - 1; i >= 0; i--) { ... }   // primeiro que tiver o arquivo vence
```

> **Índice 0 = menor prioridade. Último índice = maior prioridade.**

Mudar a ordem visual não bastaria — mas mudar a `enabled` muda o `ResourceManager` de verdade,
porque é literalmente a mesma lista.

## 4. Qual método faz o empilhamento

`ResourcePackProfile.InsertionPosition#insert(List, T, Function, boolean)`:

- `TOP` → varre do fim e insere no **final** da lista (prioridade máxima)
- `BOTTOM` → varre do início e insere no **começo** (prioridade mínima)

## 5. Ponto de injeção do Mixin

`ResourcePackManager#buildEnabledProfiles(Collection<String>) → List<ResourcePackProfile>` (privado).

É o **único** ponto de convergência: tanto `setEnabledProfiles(...)` quanto `scanPacks()` gravam o
retorno dele direto no campo `enabled`:

```
setEnabledProfiles(ids) ─┐
                         ├─→ buildEnabledProfiles(ids) ──→ this.enabled
scanPacks() ─────────────┘
```

O mod injeta em `@At("RETURN")` e reescreve a lista pronta. Um único Mixin, um único método.

## 6. Por que continua valendo depois de um resource reload

Porque não guardamos estado: **toda** reconstrução da pilha passa por `buildEnabledProfiles`.
Isso cobre inicialização, a tela de packs do vanilla, o servidor mandando um pack, o servidor
**trocando** o pack, e a desconexão. O reload que o `ReloadScheduler` dispara acontece *depois*
que a ordem já foi corrigida, então o `ResourceManager` nasce com a prioridade certa.

Na desconexão os packs `server/...` deixam de ser fornecidos, a pilha é reconstruída sem eles e o
comportamento vanilla volta sozinho.

---

## Instalação

1. Instale o **Fabric Loader 0.19.2+** para **Minecraft 1.21.11**.
2. Coloque em `.minecraft/mods/`:
   - `resourcepack-priorizer-1.0.0.jar`
   - [Fabric API](https://modrinth.com/mod/fabric-api) para 1.21.11 (`0.141.6+1.21.11` ou mais novo)

Dependências: Fabric Loader, Fabric API, Java 21+. Nada no servidor.

## Como usar

Abra a config por qualquer um dos dois caminhos:

- tecla **P** (configurável em Opções → Controles → Diversos)
- botão **"Prioridade..."** dentro da tela de Resource Packs do vanilla

Opções:

| Opção | Padrão | O que faz |
|---|---|---|
| Local sobrepõe servidor | **LIGADO** | packs locais ficam acima dos packs do servidor |
| Manter prioridade ao trocar pack | **LIGADO** | reaplica quando o servidor troca de pack |
| Quais packs locais | **Todos** | `Todos` ou `Somente marcados` |
| Subir também packs de mods | DESLIGADO | inclui packs built-in/de mods na promoção |

Na lista: **topo = maior prioridade**, a linha laranja marca a fronteira com os packs do servidor,
e as setas ▲▼ reordenam os packs locais entre si. **Aplicar e Recarregar** salva e recarrega os
recursos — e pula o reload se a ordem não mudou.

Config em `.minecraft/config/resourcepack-priorizer.json`:

```json
{
  "enabled": true,
  "localOverridesServer": true,
  "keepPriorityOnServerChange": true,
  "liftBuiltinPacks": false,
  "mode": "ALL_LOCAL",
  "priorityOrder": []
}
```

`priorityOrder` guarda ids de packs locais, **maior prioridade primeiro**.

---

## Compilar

```bash
export JAVA_HOME=/caminho/para/jdk21+     # obrigatório: o Gradle precisa de JVM 17+
./gradlew build
```

O jar sai em **`build/libs/resourcepack-priorizer-1.0.0.jar`**
(ignore o `-sources.jar`, que é só o código-fonte).

Rodar um client de teste: `./gradlew runClient`

### Importar no IntelliJ IDEA

1. **File → Open** e selecione a pasta do projeto (o `build.gradle.kts` é detectado sozinho).
2. Aceite a importação Gradle.
3. **File → Project Structure → SDK**: escolha um **JDK 21 ou superior**.
4. Espere o `genSources` do Loom terminar (dá para navegar na source do Minecraft depois).
5. Rode pela config **Minecraft Client** que o Loom gera, ou pela task Gradle `runClient`.

### Estrutura

```
resourcepack-priorizer/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── README.md
└── src/main/
    ├── java/com/github/udxkr/packpriority/
    │   ├── PackPriorityMod.java              entrypoint, keybind, botão na tela de packs
    │   ├── config/
    │   │   ├── PackPriorityConfig.java
    │   │   └── PackPriorityConfigManager.java   JSON em config/
    │   ├── core/
    │   │   ├── PackKind.java                 LOCAL / SERVER / BUILTIN
    │   │   └── PackPriorityEngine.java       a lógica de reordenação
    │   ├── mixin/
    │   │   └── ResourcePackManagerMixin.java o único Mixin
    │   └── screen/
    │       ├── PackPriorityScreen.java
    │       └── PackRow.java
    └── resources/
        ├── fabric.mod.json
        ├── resourcepack-priorizer.mixins.json
        └── assets/resourcepack-priorizer/lang/{en_us,pt_br}.json
```

---

## Plano de teste

### A. Preparar dois packs em conflito

Use o mesmo caminho de arquivo nos dois, com texturas **visivelmente diferentes**:

```
assets/minecraft/textures/block/diamond_block.png
```

- no seu pack local (ex.: `WFG PACK 2025`) → vermelho
- no pack do servidor → azul

Um `pack.mcmeta` válido em cada:

```json
{ "pack": { "pack_format": 46, "description": "teste" } }
```

### B. Executar

1. Ative o pack local em Opções → Resource Packs.
2. Conecte no servidor e aceite o pack dele.
3. Olhe um bloco de diamante.

| Estado | Resultado esperado |
|---|---|
| Override **LIGADO** | textura **vermelha** (a sua) |
| Override **DESLIGADO** | textura **azul** (a do servidor) |

Alterne pela tecla **P** → *Aplicar e Recarregar* e a textura deve trocar sem reiniciar o jogo.

### C. Conferir a prioridade no log

O mod escreve a pilha final em `.minecraft/logs/latest.log` toda vez que ela muda:

```
[Render thread/INFO] (ResourcePack Priorizer) Resource pack priority (highest first):
  #1 [LOCAL] file/WFG PACK 2025.zip
  #2 [SERVER] server/00000001/4b94ab43-1d5b-3872-9c89-bd040e9ab892
  #3 [SERVER] server/00000002/9c70933a-ff6b-3a6d-88c6-87a6cbb6b765
  #4 [BUILTIN] vanilla
```

**`#1` é a maior prioridade.** Se o seu pack aparece acima das linhas `[SERVER]`, está funcionando.

Dá para conferir também pela linha do próprio Minecraft:

```
[Render thread/INFO] (Minecraft) Reloading ResourceManager: vanilla, …, server/00000001/…, file/WFG PACK 2025.zip
```

Aqui a ordem é a **inversa**: é a lista `enabled` crua, então o **último** é o de maior prioridade.
O seu pack tem que aparecer **depois** dos `server/…`.

### D. Casos de borda

| Cenário | Esperado |
|---|---|
| Servidor troca o pack em jogo | prioridade reaplicada sozinha |
| Servidor manda vários packs | todos ficam abaixo dos locais, na ordem original entre si |
| Pack obrigatório do servidor | continua ativo e obrigatório, só perde a prioridade |
| Desconectar | volta ao comportamento vanilla |
| Pack em `.zip` e em pasta | ambos suportados |
| Sem servidor / singleplayer | mod não faz nada, sem reload extra |

---

## Escopo

O mod **não**: remove o pack do servidor, bloqueia a conexão, altera arquivos do servidor, manda
arquivos modificados para o servidor, falsifica respostas, nem mexe em autenticação ou download.

Ele só reordena a pilha de recursos **no cliente**, que é uma decisão puramente local de
renderização — o mesmo tipo de mudança que a tela de Resource Packs do vanilla já faz, só que sem a
trava `fixedPosition` que o servidor pede.

## Licença

CC0-1.0 — veja [LICENSE](LICENSE).
