# Guia Rápido para ziviDomeLive

Parabéns pela instalação do **ziviDomeLive**! Se você ainda não completou a instalação, consulte os [Passos de Instalação](../installation/installation-steps.md) para configurar tudo. Com a instalação pronta, você já está pronto para começar a criar visuais imersivos diretamente no Processing.

---

## Passo 1: Configurando Seu Sketch

Para começar, abra o Processing e crie um novo sketch. A configuração do **ziviDomeLive** é simples e permite explorar rapidamente suas funcionalidades principais.

Primeiro, importe o **ziviDomeLive** e qualquer dependência essencial no início do seu sketch. Isso garantirá que todas as funcionalidades da biblioteca estejam acessíveis e prontas para uso.

```java
import com.victorvalentim.zividomelive.*;
import controlP5.*;
import codeanticode.syphon.*;
import spout.*;
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
	currentScene = new Scene(ziviDome);

	// Define a currentScene como a cena ativa dentro do ziviDomeLive
	ziviDome.setScene(currentScene);
}
```

Concluindo esse passo, o ambiente está pronto, e o **ziviDomeLive** está preparado para gerenciar seus visuais.

---

## Passo 2: Ativando o Módulo de Renderização

Com o **ziviDomeLive** inicializado, é hora de começar a renderizar! Na função `draw()` principal do sketch, você pode chamar a função `ziviDome.draw()` para renderizar a cena atual. Isso garante que a cena seja desenhada corretamente em cada frame.

```java
// Função de desenho que é chamada repetidamente para renderizar o conteúdo da tela
void draw() {
	// Chama o método draw() do ziviDomeLive para processar e renderizar o conteúdo da cena atual
	ziviDome.draw();
}
```
___

## Passo 3: Ativando os Controles de Interação Básica

A biblioteca **ziviDomeLive** oferece uma maneira intuitiva de lidar com interações do usuário dentro das cenas, possibilitando uma resposta em tempo real tanto para configurações visuais simples quanto complexas. Você pode ativar os controles definindo funções de evento diretamente no seu sketch principal do Processing, gerenciando interações como entradas de teclado, eventos de mouse e controles de interface usando o **ControlP5** de maneira organizada e eficiente.

Veja como ativar a interação básica para sua cena usando as seguintes funções:

1. **Entrada pelo Teclado**:
   A função `keyPressed()` permite que o **ziviDomeLive** capture e gerencie eventos do teclado. Dentro desta função, qualquer entrada de teclado pode ser encaminhada para a cena atual, permitindo respostas específicas para teclas pressionadas.
2. **Eventos do Mouse**:
   A função `mouseEvent()` captura e processa eventos do mouse, como cliques e movimentos. Assim como a entrada do teclado, os eventos do mouse podem ser direcionados para a cena atual para interações personalizadas.
3. **Eventos de Controle**:
   O **ControlP5** é uma biblioteca de interface do usuário que permite criar controles personalizados, como botões, sliders e caixas de texto. A função `controlEvent()` é usada para lidar com eventos gerados por esses controles, permitindo que você ajuste parâmetros visuais em tempo real.

```java
// Função que responde aos eventos de teclas pressionadas
void keyPressed() {
	// Passa o evento de tecla pressionada para o ziviDomeLive, permitindo que ele processe a interação
	ziviDome.keyPressed();

	// Verifica se existe uma cena ativa (currentScene) configurada
	if (currentScene != null) {
		// Encaminha o evento de tecla pressionada para a cena atual, permitindo que a cena responda ao evento
		currentScene.keyPressed(key);
	}
}

// Função que responde a eventos de mouse
void mouseEvent(processing.event.MouseEvent event) {
	// Passa o evento de mouse para o ziviDomeLive para processamento, possibilitando interatividade com o mouse
	ziviDome.mouseEvent(event);

	// Verifica se existe uma cena ativa (currentScene) configurada
	if (currentScene != null) {
		// Encaminha o evento de mouse para a cena atual, permitindo que a cena responda ao evento de forma personalizada
		currentScene.mouseEvent(event);
	}
}

// Função que responde a eventos de controle gerados pelo ControlP5
void controlEvent(controlP5.ControlEvent theEvent) {
	// Passa o evento de controle para o ziviDomeLive para processamento, permitindo interação com elementos da interface ControlP5
	ziviDome.controlEvent(theEvent);
}
```
___

## Passo 4: Criando uma Classe de Cena Básica

O núcleo do **ziviDomeLive** gira em torno das cenas, que permitem organizar diferentes componentes visuais e alternar entre eles de forma prática.

Para começar, crie uma classe de cena básica implementando a interface **Scene**. Defina a configuração inicial da cena, incluindo cores de fundo, formas ou objetos 3D que deseja exibir. No conteúdo principal da cena, utilize a função `sceneRender()` para definir o que deve ser desenhado em cada frame.

```java
// Define uma classe chamada Scene que implementa a interface Scene
class Scene implements Scene {
	// Declara uma variável do tipo zividomelive chamada parent, que representa uma referência à instância principal de ziviDomeLive
	zividomelive parent;

	// Construtor da classe Scene1 que recebe uma instância de zividomelive como parâmetro e atribui essa instância ao atributo parent
	Scene1(zividomelive parent) {
		this.parent = parent;
	}

	// Método para configurar a cena
	public void setupScene() {
		// Configuração específica da cena, se necessário
	}

	// Método responsável por renderizar a cena
	public void sceneRender(PGraphics pg) {
		// Lógica de renderização da cena
	}

	// Método que responde a eventos de tecla pressionada
	public void keyPressed(char key) {
		// Lógica de resposta a teclas pressionadas
	}

	// Método que responde a eventos de mouse
	public void mouseEvent(MouseEvent event) {
		// Lógica de resposta a eventos de mouse
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
