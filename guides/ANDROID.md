# Corre Nengue para Android

## Versão atual

- Versão: **1.10.1** · código Android **27**.
- Android mínimo: **5.0**, API 21.
- SDK de compilação e destino: **35**.
- Java: **17** no aplicativo; ambiente validado com **JDK 21**.
- Gradle Wrapper: **8.11.1** · Android Gradle Plugin: **8.9.2**.

## Preparar e compilar

Configure o JDK em `JAVA_HOME`. Instale o Android SDK Platform 35 e configure
o SDK em `ANDROID_HOME` ou no arquivo local `local.properties`:

```properties
sdk.dir=C:/caminho/Android/Sdk
```

Na raiz do repositório:

```powershell
.\gradlew.bat :app:assembleDebug
```

Saída: `app/build/outputs/apk/debug/app-debug.apk`.
O APK de debug usa assinatura de desenvolvimento. Dependências são baixadas
na primeira compilação. `local.properties`, caches e saídas de build são ignorados pelo Git.

## Jogar

Toque em **Nengue Play** e depois na tela para começar. Cada toque dá impulso ao
personagem. Passe pelos vãos das paredes e evite o chão. Os pontos são **Nencoins**;
o recorde fica salvo no aparelho.

As configurações incluem paletas, cenários, sons e ajustes de movimento.
Música e sirene possuem controles separados. O aplicativo pausa o áudio ao sair
de primeiro plano.

## Bônus

Os itens precisam ser coletados no vão de uma parede à frente. A primeira
aparição é a banana a partir de 20 Nencoins, seguida de celular e bazuca.
Depois, o tipo é sorteado sem repetir o anterior. As aparições são separadas
por intervalos de 10–25 Nencoins; os efeitos não se sobrepõem.

- **Banana:** ativa Sayajin e proteção contra paredes por 10 segundos.
- **Celular:** aumenta o vão em até 50%, limitado a 64 pixels nativos extras.
  Ao terminar, o fechamento é gradual; um par em travessia espera o personagem sair.
- **Bazuca:** dispara contra paredes automaticamente por 10 segundos. O par atingido
  sai do desenho e das colisões, com flash e tijolos caindo. Volta ao ser reciclado
  fora da tela. A pontuação continua dependendo de ultrapassar o obstáculo.

Todos duram 10 segundos de partida ativa e congelam durante a pausa.
Os bônus não protegem contra o chão.

## Gesto secreto

Desenhe **↓ →** com um dedo, formando um L contínuo e soltando só no final.
Faça no centro da tela. Vale no menu, na espera, durante a partida, na tela de
derrota e nas Configurações. Depois da derrota, a ativação vale para a próxima partida.

A mensagem confirma ativação ou desativação. O indicador fica visível enquanto
o modo estiver ligado. Repita o mesmo L para desligar. O estado permanece no
processo atual do aplicativo e não é salvo em perfis.

## Verificações

Com Python disponível e `JAVA_HOME` configurado, os scripts compilam as classes
reais de lógica usando o JDK. Exemplos:

```powershell
python scripts/test_powerups.py
python scripts/test_wall_status.py
python scripts/test_bazooka_targeting.py
python scripts/test_secret_l_gesture.py
python scripts/test_secret_gesture_touch.py
python scripts/test_sky_traffic.py
```

Há também verificações de colisões e áudio. Os testes com APIs Android simuladas
validam os controladores isolados. A compilação e esses testes foram realizados;
o teste em aparelho permanece uma etapa separada.
