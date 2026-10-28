"""Translate existing UI literals without changing preference keys or resource IDs."""
from pathlib import Path
import re
import html

ROOT = Path(__file__).resolve().parents[1] / 'app/src/main'
LABELS = {
    'Back':'Voltar', 'SETTINGS':'CONFIGURAÇÕES', 'Live Preview':'Prévia em tempo real',
    'LIVE':'AO VIVO', 'Profiles & Management':'Perfis e ajustes',
    'Slot 1':'Perfil 1','Slot 2':'Perfil 2','Slot 3':'Perfil 3',
    'Save':'Salvar','Load':'Carregar','Randomize':'Sortear ajustes','Default':'Restaurar padrão',
    'Clear All Data':'Apagar todos os perfis','World & Atmosphere':'Cidade e ambiente',
    'Dynamic Cycle':'Alternar dia e noite','Background Theme':'Tema do cenário',
    'Day':'Dia','Night':'Noite','Random':'Aleatório','Weather Effects':'Clima',
    'Clear':'Limpo','Rain':'Chuva','Storm':'Tempestade','Snow':'Neve',
    'Lightning Events (Storm Only)':'Raios durante a tempestade',
    'Strike Nencoins (e.g. 10,20):':'Raios aos Nencoins (ex.: 10,20):',
    'Mega Strike Nencoins:':'Nencoins para o super-raio:',
    'Visuals & Effects':'Visual e efeitos','Personagem: Nengue':'Cor do personagem',
    'Yellow':'Amarelo','Blue':'Azul','Red':'Vermelho','White':'Branco','Pink':'Rosa',
    'Black':'Preto','Purple':'Roxo','Orange':'Laranja','Classic':'Original',
    'Rainbow Bird':'Personagem arco-íris','Bird Trail':'Rastro do personagem',
    'Screen Shake':'Tremor da tela','Shake Strength:':'Intensidade do tremor:',
    'Motion Blur':'Desfoque de movimento','Blur Strength:':'Intensidade do desfoque:',
    'Game Over Opacity %:':'Opacidade do fim de jogo (%):',
    'Tier 1 Nencoins':'Nencoins do nível 1','Tier 2 Nencoins':'Nencoins do nível 2',
    'Chance %':'Chance (%)','Game Physics':'Física do jogo','Game Speed':'Velocidade do jogo',
    'Gravity':'Gravidade','Jump Strength':'Força do salto','Hang Delay':'Tempo de flutuação',
    'Bird Size':'Tamanho do personagem','Hitbox Scale':'Área de colisão',
    'Height Variation':'Variação de altura','Speed Flux':'Variação de velocidade',
    'Sky Speed':'Velocidade do cenário','Ground Speed':'Velocidade do chão',
    'Modes & Cheats':'Modos e vantagens','FB v1.3 Mode':'Modo clássico v1.3',
    'Authentic physics. Overrides settings. Not working, unreleased, highly experimental.':
        'Física clássica. Substitui os ajustes personalizados. Modo experimental, ainda em desenvolvimento.',
    'God Mode (No Clip)':'Modo invencível (sem colisão)',
    'Ghost Mode (Transparent)':'Modo fantasma (transparente)',
    'Jetpack Mode':'Modo mochila a jato','Infinite Flap':'Saltos infinitos',
    'Drunk Mode':'Modo cambaleante','Drunk Strength:':'Intensidade do balanço:',
    'Upside Down World':'Mundo de cabeça para baixo','System':'Sistema e áudio',
    'Sound Effects':'Efeitos sonoros','Vibration':'Vibração',
    'Bird Wing Animations':'Animação do personagem','Hide Gear Icon In-Game':'Ocultar ajustes na partida',
}

JAVA = {
    'Settings Disclaimer':'Sobre os ajustes',
    'Adjusting settings can potentially make the game unstable. Proceed with caution.':
        'Alguns ajustes extremos podem afetar a partida. Você pode restaurar o padrão a qualquer momento.',
    'I Understand':'Entendi', 'Reset All Settings':'Restaurar todos os ajustes',
    'Are you sure you want to reset all current settings to their defaults? This will not affect your saved profiles.':
        'Restaurar os ajustes atuais para o padrão? Seus perfis salvos serão mantidos.',
    'Reset':'Restaurar','Cancel':'Cancelar',
    'All settings have been reset to default.':'Os ajustes foram restaurados para o padrão.',
    'Reset All Profiles':'Apagar todos os perfis',
    'Are you sure you want to delete all 3 of your saved profiles? This cannot be undone.':
        'Apagar os três perfis salvos? Essa ação não pode ser desfeita.',
    'All profiles have been reset.':'Todos os perfis foram apagados.',
    'Chaos engaged! All settings randomized.':'Bagunça ativada! Todos os ajustes foram sorteados.',
    'Enable Original Mode?':'Ativar o modo clássico?',
    'This will override physics, spacing, and animations to mathematically match the original v1.3 experience. Custom settings will be ignored while this mode is active.':
        'Este modo substitui a física, os espaços e as animações para reproduzir a versão clássica v1.3. Os ajustes personalizados ficam desativados enquanto ele estiver ativo.',
    'Enable':'Ativar','Profile ':'Perfil ',' saved':' salvo',' is empty':' está vazio',
    ' loaded':' carregado','Reset Profile ':'Apagar perfil ',' reset':' apagado',
    'Are you sure you want to delete all settings saved in this profile?':
        'Apagar todos os ajustes salvos neste perfil?',
    'Display Notice':'Sobre esta tela',
    "Flappy Bird is not optimized for this screen's aspect ratio. You may encounter visual issues. For the best experience, a device with a smaller screen (and preferably Android 14+) is recommended.":
        'O jogo pode apresentar diferenças visuais nesta proporção de tela. A experiência é melhor em um celular no modo retrato.',
    'Continue':'Continuar','No error message available.':'Nenhuma mensagem de erro disponível.',
    ' Crashed':' — ocorreu um erro',
    'Invalid string operation\\n':'Operação de texto inválida\\n',
    'Invalid list operation\\n':'Operação de lista inválida\\n',
    'Invalid arithmetical operation\\n':'Operação aritmética inválida\\n',
    'Invalid toNumber block operation\\n':'Conversão numérica inválida\\n',
    'Invalid intent operation\\n':'Não foi possível abrir a tela\\n',
}

def main():
    path=ROOT/'res/layout/settings.xml'
    data=path.read_text(encoding='utf-8')
    def label(m):
        value=html.unescape(m[1])
        return 'android:text="'+html.escape(LABELS.get(value,value),quote=True)+'"'
    data=re.sub(r'android:text="([^"]*)"',label,data)
    data=data.replace('android:id="@+id/hscroll16" android:visibility="gone"','android:id="@+id/hscroll16"')
    # Existing color selector IDs remain compatible with saved profiles.
    data=re.sub(r'(android:id="@\+id/textview28".*?android:text=")[^"]*',r'\1Original',data,flags=re.S)
    data=re.sub(r'(android:id="@\+id/textview30".*?android:text=")[^"]*',r'\1Rosa',data,flags=re.S)
    # More room for the translated header and long switch labels.
    data=data.replace('android:text="CONFIGURAÇÕES"\n\t\t\t\tandroid:textSize="28sp"','android:text="CONFIGURAÇÕES"\n\t\t\t\tandroid:textSize="20sp"')
    data=data.replace('android:layout_centerInParent="true"','android:layout_alignParentEnd="true"',1)
    path.write_text(data,encoding='utf-8',newline='\n')
    for name in ['SettingsActivity.java','MainActivity.java','DebugActivity.java']:
        path=ROOT/'java/com/flappybird/recreation'/name
        data=path.read_text(encoding='utf-8')
        for old,new in JAVA.items(): data=data.replace('"'+old+'"','"'+new+'"')
        path.write_text(data,encoding='utf-8',newline='\n')

if __name__ == '__main__': main()
