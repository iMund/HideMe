# Hide-Me

Projeto Fabric mod voltado para esconder jogadores no servidor. Este repositorio ja esta preparado para levantar um servidor dedicado de desenvolvimento.

## Funcionalidades
- Oculta operadores e jogadores com a permissao `hide-me.use` **apenas** da tab list; entidades continuam visiveis no mundo.
- Remove as mensagens padrao de entrada/saida ("<jogador> entrou/saiu do jogo") para jogadores ocultos, mantendo o chat limpo.

## Requisitos
- Java 21 instalado e configurado no `PATH`;
- Gradle Wrapper (`gradlew`) incluido no projeto;
- (Opcional) IntelliJ IDEA ou outro IDE compativel com Fabric.

## Executando o servidor Fabric
1. **Primeira execucao**
   - CLI direto: `./gradlew runServer --args="--nogui"`
   - VS Code: tarefa `Gradle: runServer` (atalho disponivel no VS Code > Terminal > Run Task).
2. Aguarde a geracao da pasta `run\` e edite o arquivo `run\eula.txt`, alterando `eula=false` para `eula=true`.
3. Reexecute o comando ou tarefa. O servidor iniciara com o mod carregado e sem interface grafica (`--nogui`).
4. Para encerrar, utilize `Ctrl+C` no terminal.

### Depuracao
- Inicie a tarefa `Gradle: runServer (debug)` para subir o servidor com o debugger aguardando em `localhost:5005`.
- Use o launch do VS Code `Attach to Fabric runServer` para conectar a depuracao.

## Estrutura dos entrypoints
- `com.tavares.hideme.Main`: inicializacao comum (cliente + servidor).
- `com.tavares.hideme.HideMeServer`: inicializacao especifica do servidor dedicado.
- `com.tavares.hideme.HideMeDataGenerator`: ponto de entrada do gerador de dados.

## Build do mod
Execute `./gradlew build`. O artefato final ficara em `build/libs/`.

## Proximos passos sugeridos
- Validar o comportamento com o plugin/gestor de permissoes utilizado no servidor (ex.: LuckPerms).
- Adicionar automatizacao de testes ou cenarios de integracao caso novas regras de sigilo sejam criadas.


