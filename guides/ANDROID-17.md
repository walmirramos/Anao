# Compatibilidade gráfica no Android 17

Versão 1.4.1 preparada para contornar o relato de texturas brancas.

- `values-v37/rendering.xml` desativa aceleração de hardware nas atividades MainActivity e GearslogoActivity a partir de API 37; versões anteriores mantêm o renderizador acelerado.
- Recursos PNG usam explicitamente ARGB_8888, memória mutável acessível pela CPU e alpha pré-multiplicado.
- O Paint compartilhado volta ao estado neutro em cada quadro.
- Escurecimento automático do sistema é desativado para preservar as cores da arte.
- Regras, bônus, abertura e arquivos originais de arte são preservados.

É uma correção de compatibilidade, sem confirmação da causa específica no aparelho. O caminho por software pode consumir mais CPU; validar fluidez e texturas no dispositivo afetado. Compilação e política de recursos do APK verificadas: aceleração habilitada no padrão e desabilitada na configuração v37.

Referência oficial: https://developer.android.com/topic/performance/views/hardware-accel-views

## Validação local

Instalação no emulador Android 17 / API 37 concluída. Abertura e menu inspecionados por capturas: logo, personagem, fábrica e carros renderizaram com cores e transparência. O emulador apresentou ANRs do Pixel Launcher e System UI; não foi possível concluir uma sessão de gameplay nem validar desempenho. Não há reprodução confirmada do defeito original no modelo do usuário.

Testes de estado dos bônus e geometria fixa da abertura passaram. O APK foi inspecionado para confirmar a resolução do booleano de aceleração por versão.
