# Guia Rápido para ziviDomeLive

Parabéns pela instalação do **ziviDomeLive**! Se você ainda não completou a instalação, consulte os [Passos de Instalação](../installation/installation-steps.md) para configurar tudo. Com a instalação pronta, você já está pronto para começar a criar visuais imersivos diretamente no Processing.

---

## Passo 1: Configurando Seu Sketch

Para começar, abra o Processing e crie um novo sketch. A configuração do **ziviDomeLive** é simples e permite explorar rapidamente suas funcionalidades principais.

Primeiro, importe o **ziviDomeLive** e qualquer dependência essencial no início do seu sketch. Isso garantirá que todas as funcionalidades da biblioteca estejam acessíveis e prontas para uso.

```java
import com.victorvalentim.zividomelive.*;
import processing.opengl.PGraphicsOpenGL;
```

Em seguida, inicialize o **ziviDomeLive** criando uma instância da biblioteca. Essa instância será a base do seu ambiente imersivo, facilitando o gerenciamento e a renderização das cenas.

```java
// Declara uma variável ziviDome do tipo zividomelive
// Essa variável será usada para instanciar e controlar a biblioteca ziviDomeLive
zividomelive ziviDome;

// Declara uma variável currentScene do tipo Scene
// Essa variável armazenará a cena atual que está sendo renderizada e interagindo com o ziviDomeLive
Scene currentScene; 
```
Nas funções `settings()` e `setup()`, defina as dimensões da tela e o modo de renderização 3D. Depois, chame a função de configuração do **ziviDomeLive** para inicializá-lo corretamente. Com esses passos, o ambiente está pronto, e o **ziviDomeLive** está preparado para gerenciar seus visuais.

```java
// Função de configuração das definições de tela
void settings() {
	// Define o tamanho da janela e ativa o modo de renderização 3D (P3D)
	size(1280, 720, P3D);
}

// Função de configuração inicial do sketch
void setup() {
	// Cria uma nova instância do ziviDomeLive, passando a referência do sketch atual
	ziviDome = new zividomelive(this);

	// Configura o ziviDomeLive, inicializando suas variáveis e preparando-o para renderizar
	ziviDome.setup();

	// Cria uma nova instância de uma cena chamada currentScene, associando-a ao ziviDomeLive
	currentScene = new Scene1(ziviDome);

	// Define a currentScene como a cena ativa dentro do ziviDomeLive
	ziviDome.setScene(currentScene);
}
```

Concluindo esse passo, o ambiente está pronto, e o **ziviDomeLive** está preparado para gerenciar seus visuais.

---

## Passo 2: Deixando a Biblioteca Renderizar

O construtor de `zividomelive` registra seu próprio hook `draw` no Processing. Não chame `ziviDome.draw()` no sketch, pois isso executa o pipeline duas vezes por frame. O sketch pode manter um `draw()` vazio para lógica futura:

```java
void draw() {
	// ziviDomeLive renderiza automaticamente.
}
```
___

## Passo 3: Ativando os Controles de Interação Básica

A biblioteca registra automaticamente os hooks de teclado e mouse do Processing e encaminha cada evento para a cena ativa. O painel ControlP5 interno encaminha seus eventos pelo mesmo contrato de `Scene`. Implemente apenas os callbacks necessários na cena; não os encaminhe novamente pelo sketch principal.

Veja como ativar a interação básica para sua cena usando as seguintes funções:

1. **Entrada pelo Teclado**:
   `keyEvent()` recebe os eventos depois que a biblioteca processa seus atalhos globais.
2. **Eventos do Mouse**:
   `mouseEvent()` recebe cliques, movimento, arraste e roda do mouse.
3. **Eventos de Controle**:
   `controlEvent()` recebe eventos do painel ControlP5 interno da biblioteca.

```java
public void keyEvent(processing.event.KeyEvent event) {
	if (event.getAction() == processing.event.KeyEvent.PRESS) {
		println("Tecla pressionada: " + event.getKey());
	}
}

public void mouseEvent(processing.event.MouseEvent event) {
	// Trate a entrada de mouse da cena.
}

public void controlEvent(controlP5.ControlEvent event) {
	// Trate eventos do painel interno relevantes para esta cena.
}
```
___

## Passo 4: Criando uma Classe de Cena Básica

O núcleo do **ziviDomeLive** gira em torno das cenas, que permitem organizar diferentes componentes visuais e alternar entre eles de forma prática.

Para começar, crie uma classe de cena básica implementando a interface **Scene**. Defina a configuração inicial da cena, incluindo cores de fundo, formas ou objetos 3D que deseja exibir. No conteúdo principal da cena, utilize a função `sceneRender()` para definir o que deve ser desenhado em cada frame.

```java
class Scene1 implements Scene {
	zividomelive parent;

	Scene1(zividomelive parent) {
		this.parent = parent;
	}

	@Override
	public void setupScene() {
		// Configuração inicial opcional da cena.
	}

	@Override
	public void sceneRender(PGraphicsOpenGL pg) {
		pg.background(0);
		pg.box(200);
		// A biblioteca controla beginDraw() e endDraw().
	}
}
```

Depois que a classe de cena estiver definida, defina-a como a cena ativa no **ziviDomeLive**, atribuindo-a na função `setup()`. Isso permite que o **ziviDomeLive** gerencie a renderização e qualquer evento de interação, como pressionamento de teclas, diretamente na sua cena.

---
## Passo 5: Executando e Interagindo com o Sketch

Após configurar e atribuir sua cena, você está pronto para executar o sketch. Basta clicar no botão Run no Processing e assistir o **ziviDomeLive** dar vida à sua cena.

Com o sketch em execução, você pode interagir usando entradas de teclado ou outros eventos do Processing. Como o **ziviDomeLive** suporta funcionalidade interativa, você pode adicionar controles facilmente, experimentar visuais dinâmicos ou alterar parâmetros em tempo real.

___

## Resumo Geral

Esses 5 passos formam a base essencial para o uso da biblioteca **ziviDomeLive** no Processing, habilitando recursos de visualização imersiva e controle da interface. Com essa configuração, ziviDomeLive está preparado para gerenciar cenas e interações, oferecendo suporte completo para experiências visuais imersivas.
___

## O Que Vem a Seguir?

Agora que você configurou uma cena básica, sinta-se à vontade para explorar recursos adicionais. Experimente adicionar novas cenas, integrar com ferramentas externas como o **Syphon** ou **Spout** para compartilhamento em tempo real, ou configurar interfaces de usuário personalizadas com o **ControlP5**. O **ziviDomeLive** oferece uma estrutura flexível para experimentar e criar experiências visuais dinâmicas que respondem à sua interação.
