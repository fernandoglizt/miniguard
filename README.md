# Mini-Guard

Detecção de movimento via webcam + alerta no Telegram. Ideal para lojas pequenas protegerem o ambiente à noite.

## Passo a passo rápido

### 1. Clonar e compilar
```bash
git clone https://github.com/<seu-user>/mini-guard.git
cd mini-guard
./gradlew run             # Windows: gradlew.bat run
```

### 2. Criar um bot no Telegram
1. Abra o chat **@BotFather** → envie `/newbot` → informe nome e username.  
2. Copie o **token** gerado (`123456:ABC...`).

### 3. Descobrir o *chat ID*
1. Envie qualquer mensagem para o novo bot (ou adicione-o a um grupo).  
2. No navegador, acesse  

   ```
   https://api.telegram.org/bot<SEU_TOKEN>/getUpdates
   ```
3. No JSON retornado procure `"chat":{"id":<número>}`.  
   * Individual → número positivo  
   * Grupo → número negativo (ex.: `-987654321`)

### 4. Primeira execução
Ao rodar `./gradlew run` o programa pedirá:
```
Enter TELEGRAM BOT TOKEN:
Enter TELEGRAM CHAT ID:
```
Ele cria o arquivo `telegram.properties` com esses dados.

### 5. Usar
* Depois do “Mini-Guard armed” deixe o terminal aberto.  
* Qualquer movimento que gere > 5 000 pixels brancos dispara **bip** e mensagem no Telegram.

### 6. Ajustes rápidos
Abra `Main.java` e altere, se necessário:
| Constante | Valor padrão | Função |
|-----------|--------------|--------|
| `MOTION_THRESHOLD_PX` | 5000 | Sensibilidade |
| `FRAME_SLEEP_MS`      | 50   | FPS (ms entre capturas) |

Recompile com `./gradlew run` ou `./gradlew shadowJar`.

Pronto!
