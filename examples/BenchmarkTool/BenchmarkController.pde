enum BenchmarkState {
  IDLE,
  WARMUP,
  WARMUP_FINISHING,
  READY,
  MEASURING,
  TRANSITION_BASELINE,
  TRANSITION_APPLYING,
  TRANSITION_RECOVERY,
  MEASUREMENT_FINISHING,
  COMPLETE,
  FAILED
}

class BenchmarkController {
  final PApplet app;
  final ziviDomeLive dome;
  final SceneManager scenes;
  final Scene[] sceneOptions;
  final OutputManager outputs;
  final ControlP5 cp5;

  final RenderMode[] renderModes = {
      RenderMode.STANDARD,
      RenderMode.DOMEMASTER,
      RenderMode.EQUIRECTANGULAR,
      RenderMode.SKYBOX
  };

  final int[] resolutions = {
      1024,
      2048,
      3072,
      4096
  };

  DropdownList renderModeDropdown;
  DropdownList resolutionDropdown;
  DropdownList sceneDropdown;
  Toggle previewToggle;
  Toggle gpuToggle;
  Toggle ndiToggle;
  Toggle syphonToggle;
  Toggle spoutToggle;
  Numberbox warmupFramesInput;
  Numberbox measurementFramesInput;
  Textlabel systemLabel;
  Textlabel liveLabel;
  Textlabel pipelineLabel;
  Textlabel outputLabel;
  Textlabel resultPathLabel;
  Chart frameChart;

  BenchmarkState state = BenchmarkState.IDLE;

  int warmupCompleted;
  int measurementCompleted;

  int configuredWarmupFrames = 600;
  int configuredMeasurementFrames = 1800;
  int configuredTransitionBaselineFrames = 120;
  int configuredTransitionPostFrames = 240;
  int configuredBenchmarkFps = 1000;

  boolean configuredBenchmarkNdi;
  boolean benchmarkOutputDemandActive;
  boolean warmupOnly;
  boolean suiteActive;

  int suiteIndex;

  List<BenchmarkSuitePlan.Scenario> suiteScenarios =
      new ArrayList<BenchmarkSuitePlan.Scenario>();

  BenchmarkSuitePlan.Scenario activeSuiteScenario;
  BenchmarkSuiteResultWriter.Session suiteSession;

  String automationSuiteName;
  String automationSceneName;

  boolean automationStarted;
  boolean exitAfterSuite;

  GpuTimerPolicy configuredGpuTimerPolicy =
      GpuTimerPolicy.ARCHITECTURE_AWARE;

  int transitionBaselineCompleted;
  int transitionPostCompleted;
  int transitionFrameIndex;

  String measurementStatus = "SUPPORTED";
  String measurementReason = "";

  BenchmarkResultWriter.Run currentRun;
  PerformanceSnapshot lastSnapshot;
  BenchmarkResultWriter.Run lastRun;

  Path lastExportDirectory;
  Path outputRoot;

  long ndiCapturedBefore;
  long ndiSentBefore;
  long ndiDroppedBefore;
  long ndiFailedBefore;

  BenchmarkController(
      PApplet app,
      ziviDomeLive dome,
      SceneManager scenes,
      Scene[] sceneOptions) {

    this.app = app;
    this.dome = dome;
    this.scenes = scenes;
    this.sceneOptions = sceneOptions;
    this.outputs = dome.getOutputManager();
    this.outputRoot = resolveOutputRoot();

    cp5 = new ControlP5(app);

    createInterface();
    configureAutomation();

    refreshStaticLabels(
        "IDLE - configure a scenario and press START");
  }

  void createInterface() {
    int panelWidth = 440;

    Group panel = cp5.addGroup("BENCHMARK_PANEL")
        .setPosition(app.width - panelWidth - 12, 12)
        .setWidth(panelWidth)
        .setBackgroundHeight(app.height - 24)
        .setBackgroundColor(app.color(8, 12, 18, 225));

    panel.setLabel(
        "ziviDomeLive Performance Qualification");

    addHeading(panel, "SYSTEM", 12);

    systemLabel = cp5.addTextlabel("BENCH_SYSTEM_TEXT")
        .setGroup(panel)
        .setPosition(12, 30)
        .setSize(416, 48)
        .setMultiline(true)
        .setLineHeight(13);

    addHeading(panel, "TEST CONFIG", 84);

    renderModeDropdown =
        addDropdown(
            panel,
            "BENCH_RENDER_MODE",
            12,
            104,
            198);

    for (int index = 0;
         index < renderModes.length;
         index++) {

      renderModeDropdown.addItem(
          renderModes[index].name(),
          index);
    }

    renderModeDropdown.setValue(0);

    resolutionDropdown =
        addDropdown(
            panel,
            "BENCH_RESOLUTION",
            224,
            104,
            204);

    for (int index = 0;
         index < resolutions.length;
         index++) {

      resolutionDropdown.addItem(
          Integer.toString(resolutions[index]),
          index);
    }

    resolutionDropdown.setValue(1);

    sceneDropdown =
        addDropdown(
            panel,
            "BENCH_SCENE",
            12,
            138,
            198);

    for (int index = 0;
         index < sceneOptions.length;
         index++) {

      sceneDropdown.addItem(
          sceneOptions[index].getName(),
          index);
    }

    sceneDropdown.setValue(2);

    previewToggle = addToggle(
        panel,
        "BENCH_PREVIEW",
        "Floating Preview",
        224,
        138,
        dome.isShowPreview());

    gpuToggle = addToggle(
        panel,
        "BENCH_GPU",
        "GPU timer",
        344,
        138,
        gpuProfilingDefault());

    ndiToggle = addToggle(
        panel,
        "BENCH_NDI",
        "NDI",
        12,
        174,
        outputs.isNdiEnabled());

    syphonToggle = addToggle(
        panel,
        "BENCH_SYPHON",
        "Syphon",
        118,
        174,
        outputs.isSyphonEnabled());

    spoutToggle = addToggle(
        panel,
        "BENCH_SPOUT",
        "Spout",
        224,
        174,
        outputs.isSpoutEnabled());

    warmupFramesInput =
        cp5.addNumberbox(
                "BENCH_WARMUP_FRAMES")
            .setGroup(panel)
            .setPosition(12, 208)
            .setSize(198, 22)
            .setRange(0, 10000)
            .setValue(configuredWarmupFrames)
            .setMultiplier(10)
            .setScrollSensitivity(1.0f);

    warmupFramesInput.setLabel(
        "Warm-up frames");

    measurementFramesInput =
        cp5.addNumberbox(
                "BENCH_MEASUREMENT_FRAMES")
            .setGroup(panel)
            .setPosition(224, 208)
            .setSize(204, 22)
            .setRange(2, 20000)
            .setValue(configuredMeasurementFrames)
            .setMultiplier(10)
            .setScrollSensitivity(1.0f);

    measurementFramesInput.setLabel(
        "Measurement frames");

    addHeading(panel, "LIVE METRICS", 246);

    liveLabel = cp5.addTextlabel(
            "BENCH_LIVE_TEXT")
        .setGroup(panel)
        .setPosition(12, 266)
        .setSize(416, 66)
        .setMultiline(true)
        .setLineHeight(13);

    addHeading(
        panel,
        "PIPELINE BREAKDOWN",
        338);

    pipelineLabel =
        cp5.addTextlabel(
                "BENCH_PIPELINE_TEXT")
            .setGroup(panel)
            .setPosition(12, 358)
            .setSize(416, 122)
            .setMultiline(true)
            .setLineHeight(13);

    addHeading(
        panel,
        "FRAME-TIME HISTORY",
        486);

    frameChart =
        cp5.addChart(
                "BENCH_FRAME_CHART")
            .setGroup(panel)
            .setPosition(12, 506)
            .setSize(416, 115)
            .setRange(0, 50)
            .setView(Chart.LINE)
            .setStrokeWeight(1.5f)
            .addDataSet("frameMs")
            .setData(
                "frameMs",
                new float[] {0.0f});

    frameChart.setColors(
        "frameMs",
        app.color(55, 220, 170));

    addHeading(
        panel,
        "BENCHMARK CONTROL",
        628);

    addButton(
        panel,
        "BENCH_WARM_UP",
        "WARM UP",
        12,
        648,
        96);

    addButton(
        panel,
        "BENCH_START",
        "START",
        116,
        648,
        72);

    addButton(
        panel,
        "BENCH_STOP",
        "STOP",
        196,
        648,
        72);

    addButton(
        panel,
        "BENCH_RESET",
        "RESET",
        276,
        648,
        72);

    addButton(
        panel,
        "BENCH_EXPORT",
        "EXPORT",
        356,
        648,
        72);

    addButton(
        panel,
        "BENCH_RUN_SUITE",
        "RUN SUITE",
        12,
        680,
        416);

    addHeading(
        panel,
        "OUTPUT STATUS",
        720);

    outputLabel =
        cp5.addTextlabel(
                "BENCH_OUTPUT_TEXT")
            .setGroup(panel)
            .setPosition(12, 740)
            .setSize(416, 54)
            .setMultiline(true)
            .setLineHeight(13);

    resultPathLabel =
        cp5.addTextlabel(
                "BENCH_RESULT_PATH")
            .setGroup(panel)
            .setPosition(12, 802)
            .setSize(416, 48)
            .setMultiline(true)
            .setLineHeight(13);

    updateOutputLocks();
  }

  void addHeading(
      Group group,
      String text,
      int y) {

    cp5.addTextlabel(
            "BENCH_HEADING_"
            + text.replace(' ', '_'))
        .setGroup(group)
        .setPosition(12, y)
        .setText(text)
        .setColorValue(
            app.color(
                120,
                210,
                255));
  }

  DropdownList addDropdown(
      Group group,
      String name,
      int x,
      int y,
      int width) {

    return cp5.addDropdownList(name)
        .setGroup(group)
        .setPosition(x, y)
        .setSize(width, 150)
        .setBarHeight(22)
        .setItemHeight(22)
        .setType(
            DropdownList.DROPDOWN)
        .close();
  }

  Toggle addToggle(
      Group group,
      String name,
      String label,
      int x,
      int y,
      boolean value) {

    Toggle toggle =
        cp5.addToggle(name)
            .setGroup(group)
            .setPosition(x, y)
            .setSize(18, 18)
            .setValue(value);

    toggle.getCaptionLabel()
        .setText(label)
        .align(
            ControlP5.RIGHT_OUTSIDE,
            ControlP5.CENTER)
        .setPaddingX(5);

    return toggle;
  }

  void addButton(
      Group group,
      String name,
      String label,
      int x,
      int y,
      int width) {

    cp5.addButton(name)
        .setGroup(group)
        .setPosition(x, y)
        .setSize(width, 24)
        .setLabel(label);
  }

  void controlEvent(
      ControlEvent event) {

    String name = event.getName();

    if (name == null
        || !name.startsWith("BENCH_")) {
      return;
    }

    if (name.equals("BENCH_WARM_UP")) {
      startWarmupOnly();

    } else if (name.equals("BENCH_START")) {
      startRun(false);

    } else if (name.equals("BENCH_STOP")) {
      stopRun();

    } else if (name.equals("BENCH_RESET")) {
      resetRun();

    } else if (name.equals("BENCH_EXPORT")) {
      exportLastRun();

    } else if (name.equals("BENCH_RUN_SUITE")) {
      startSuite();
    }
  }

  void update() {
    if (!dome.isInitialized()) {
      systemLabel.setText(
          "Waiting for the ziviDomeLive OpenGL pipeline...");
      return;
    }

    if (!automationStarted
        && automationSuiteName != null) {

      automationStarted = true;

      startSuite(
          BenchmarkSuitePlan.Suite.parse(
              automationSuiteName));
    }

    switch (state) {
      case WARMUP:
        warmupCompleted++;

        if (warmupCompleted
            >= configuredWarmupFrames) {

          state =
              BenchmarkState.WARMUP_FINISHING;

          refreshStaticLabels(
              "WARM-UP finishing at a frame boundary");

        } else if (warmupCompleted % 120 == 0) {
          refreshStaticLabels(
              "WARM-UP "
              + warmupCompleted
              + "/"
              + configuredWarmupFrames);
        }

        break;

      case WARMUP_FINISHING:
        if (warmupOnly) {
          finishWarmupOnly();
        } else {
          beginMeasurement();
        }

        break;

      case MEASURING:
        measurementCompleted++;

        if (measurementCompleted
            >= configuredMeasurementFrames) {

          state =
              BenchmarkState.MEASUREMENT_FINISHING;
        }

        break;

      case TRANSITION_BASELINE:
        transitionBaselineCompleted++;
        measurementCompleted++;

        if (transitionBaselineCompleted
            >= activeSuiteScenario.baselineFrames) {

          state =
              BenchmarkState.TRANSITION_APPLYING;
        }

        break;

      case TRANSITION_APPLYING:
        transitionFrameIndex =
            measurementCompleted;

        boolean transitionSupported =
            applyTransitionTarget(
                activeSuiteScenario.target);

        transitionPostCompleted = 1;
        measurementCompleted++;

        measurementStatus =
            transitionSupported
                ? "SUPPORTED"
                : "UNSUPPORTED";

        measurementReason =
            transitionSupported
                ? ""
                : "Requested transition output could not be enabled";

        state =
            !transitionSupported
                    || transitionPostCompleted
                        >= activeSuiteScenario.postFrames
                ? BenchmarkState.MEASUREMENT_FINISHING
                : BenchmarkState.TRANSITION_RECOVERY;

        break;

      case TRANSITION_RECOVERY:
        transitionPostCompleted++;
        measurementCompleted++;

        if (transitionPostCompleted
            >= activeSuiteScenario.postFrames) {

          state =
              BenchmarkState.MEASUREMENT_FINISHING;
        }

        break;

      case MEASUREMENT_FINISHING:
        finishMeasurement(
            measurementStatus);

        break;

      default:
        break;
    }
  }

  void startWarmupOnly() {
    if (!canStart()) return;

    suiteActive = false;
    warmupOnly = true;

    if (!configureScenario()) {
      return;
    }

    dome.enablePerformanceProfiling(
        selectedPerformanceMode(),
        sampleCapacity(),
        configuredGpuTimerPolicy);

    dome.resetPerformanceStatistics();

    warmupCompleted = 0;

    state =
        configuredWarmupFrames > 0
            ? BenchmarkState.WARMUP
            : BenchmarkState.WARMUP_FINISHING;

    refreshStaticLabels(
        "WARM-UP started; samples will be discarded");
  }

  void startRun(boolean fromSuite) {
    if (!canStart()) return;

    if (!fromSuite) {
      suiteActive = false;
      activeSuiteScenario = null;
      suiteSession = null;
    }

    warmupOnly = false;

    if (!configureScenario()) {
      suiteActive = false;
      return;
    }

    dome.enablePerformanceProfiling(
        selectedPerformanceMode(),
        sampleCapacity(),
        configuredGpuTimerPolicy);

    dome.resetPerformanceStatistics();

    warmupCompleted = 0;
    measurementCompleted = 0;
    transitionBaselineCompleted = 0;
    transitionPostCompleted = 0;
    transitionFrameIndex = 0;

    measurementStatus = "SUPPORTED";
    measurementReason = "";

    if (fromSuite
        && activeSuiteScenario != null) {

      currentRun.testType =
          activeSuiteScenario.kind.name();

      currentRun.scenarioName =
          activeSuiteScenario.name;

      if (activeSuiteScenario.kind
          == BenchmarkSuitePlan.Kind.TRANSITION) {

        currentRun.requestedMeasurementFrames =
            activeSuiteScenario.baselineFrames
            + activeSuiteScenario.postFrames;

        currentRun.transition =
            new BenchmarkResultWriter.Transition();

        currentRun.transition.from =
            activeSuiteScenario.initial.description();

        currentRun.transition.to =
            activeSuiteScenario.target.description();
      }
    }

    state =
        configuredWarmupFrames > 0
            ? BenchmarkState.WARMUP
            : BenchmarkState.WARMUP_FINISHING;

    refreshStaticLabels(
        configuredWarmupFrames > 0
            ? "WARM-UP started; measurement is isolated"
            : "MEASURING; live aggregation paused to protect samples");
  }

  boolean canStart() {
    if (!dome.isInitialized()) {
      refreshStaticLabels(
          "FAILED - renderer is not initialized");

      return false;
    }

    if (isRunning()) {
      refreshStaticLabels(
          "A benchmark is already running; press STOP first");

      return false;
    }

    return true;
  }

  boolean configureScenario() {
    configuredWarmupFrames =
        max(
            0,
            round(
                warmupFramesInput.getValue()));

    configuredMeasurementFrames =
        constrain(
            round(
                measurementFramesInput.getValue()),
            2,
            20000);

    RenderMode mode =
        selectedRenderMode();

    ViewType view =
        viewFor(mode);

    boolean outputDemand =
        suiteActive
            && activeSuiteScenario != null
            && activeSuiteScenario.initial.outputDemand;

    int resolution =
        selectedResolution();

    Scene scene =
        sceneOptions[
            selectedSceneIndex()];

    dome.setRenderMode(mode);
    dome.setCurrentView(view);
    dome.setCurrentScene(scene);
    dome.setShowPreview(
        previewToggle.getState());

    dome.resetGraphics(
        resolution);

    outputs.setNdiView(view);
    outputs.setSyphonView(view);
    outputs.setSpoutView(view);

    if (!applyOutput(
            OutputManager.OutputType.NDI,
            ndiToggle.getState(),
            "ndi")
        || !applyOutput(
            OutputManager.OutputType.SYPHON,
            syphonToggle.getState(),
            "syphon")
        || !applyOutput(
            OutputManager.OutputType.SPOUT,
            spoutToggle.getState(),
            "spout")) {

      state =
          BenchmarkState.FAILED;

      updateOutputLocks();

      refreshStaticLabels(
          "UNSUPPORTED - a requested output is unavailable; run aborted");

      refreshOutputLabel();

      clearBenchmarkOutputDemand();
      return false;
    }

    applyBenchmarkOutputDemand(
        view,
        outputDemand);

    currentRun =
        createRunMetadata(
            mode,
            view,
            resolution,
            scene);

    return true;
  }

  boolean applyOutput(
      OutputManager.OutputType type,
      boolean desired,
      String toggleName) {

    OutputManager.OutputState before =
        outputs.getOutputState(type);

    if (desired
        && before
            == OutputManager.OutputState.UNAVAILABLE) {

      return false;
    }

    boolean enabled =
        outputEnabled(type);

    if (enabled != desired) {
      outputs.toggleOutput(
          toggleName);
    }

    OutputManager.OutputState after =
        outputs.getOutputState(type);

    return !desired
        || after
            == OutputManager.OutputState.ENABLED;
  }

  void beginMeasurement() {
    dome.resetPerformanceStatistics();

    currentRun.timestamp =
        Instant.now();

    captureNdiBaseline();

    measurementCompleted = 0;
    transitionBaselineCompleted = 0;
    transitionPostCompleted = 0;

    state =
        suiteActive
                && activeSuiteScenario != null
                && activeSuiteScenario.kind
                    == BenchmarkSuitePlan.Kind.TRANSITION
            ? BenchmarkState.TRANSITION_BASELINE
            : BenchmarkState.MEASURING;

    refreshStaticLabels(
        "MEASURING; live aggregation paused to protect samples");
  }

  void finishWarmupOnly() {
    dome.disablePerformanceProfiling();
    dome.resetPerformanceStatistics();
    clearBenchmarkOutputDemand();

    state =
        BenchmarkState.READY;

    refreshStaticLabels(
        "READY - warm-up complete; press START for a measured run");
  }

  void finishMeasurement(
      String status) {

    dome.disablePerformanceProfiling();

    lastSnapshot =
        dome.getPerformanceSnapshot();

    currentRun.status =
        status;

    populateTransitionStatistics();

    captureNdiDelta(
        currentRun);

    refreshEnvironment(
        currentRun);

    clearBenchmarkOutputDemand();

    lastRun =
        currentRun;

    state =
        BenchmarkState.COMPLETE;

    updateResultViews();

    if (suiteActive) {
      if (!exportLastRun()) {
        return;
      }

      recordSuiteResult(
          currentRun.status,
          measurementReason,
          lastExportDirectory);

      suiteIndex++;

      startNextSuiteScenario();
    }
  }

  void stopRun() {
    if (!isRunning()) {
      return;
    }

    suiteActive = false;

    if (state == BenchmarkState.MEASURING
        || state == BenchmarkState.TRANSITION_BASELINE
        || state == BenchmarkState.TRANSITION_APPLYING
        || state == BenchmarkState.TRANSITION_RECOVERY
        || state == BenchmarkState.MEASUREMENT_FINISHING) {

      finishMeasurement(
          "STOPPED");

    } else {
      dome.disablePerformanceProfiling();
      dome.resetPerformanceStatistics();

      state =
          BenchmarkState.IDLE;

      refreshStaticLabels(
          "STOPPED - warm-up samples discarded");
    }

    clearBenchmarkOutputDemand();
  }

  void resetRun() {
    suiteActive = false;
    clearBenchmarkOutputDemand();

    dome.disablePerformanceProfiling();
    dome.resetPerformanceStatistics();

    lastSnapshot = null;
    lastRun = null;
    lastExportDirectory = null;
    activeSuiteScenario = null;
    suiteSession = null;

    state =
        BenchmarkState.IDLE;

    frameChart.setData(
        "frameMs",
        new float[] {0.0f});

    pipelineLabel.setText(
        "No completed measurement.");

    refreshStaticLabels(
        "IDLE - benchmark state reset");
  }

  void startSuite() {
    startSuite(
        BenchmarkSuitePlan.Suite.MODES);
  }

  void startSuite(
      BenchmarkSuitePlan.Suite suite) {

    if (!canStart()) {
      return;
    }

    String sceneName =
        automationSceneName == null
            ? sceneOptions[
                    selectedSceneIndex()]
                .getName()
            : automationSceneName;

    suiteScenarios =
        BenchmarkSuitePlan.create(
            suite,
            sceneName,
            selectedResolution(),
            resolutions,
            previewToggle.getState(),
            configuredTransitionBaselineFrames,
            configuredTransitionPostFrames,
            ndiToggle.getState());

    suiteSession =
        new BenchmarkSuiteResultWriter.Session();

    suiteSession.suite =
        suite.name();

    suiteSession.revision =
        configuredRevision();

    suiteActive = true;
    suiteIndex = 0;

    startNextSuiteScenario();
  }

  void startNextSuiteScenario() {
    while (suiteActive
        && suiteIndex
            < suiteScenarios.size()) {

      activeSuiteScenario =
          suiteScenarios.get(
              suiteIndex);

      String unsupported =
          unsupportedReason(
              activeSuiteScenario);

      if (unsupported != null) {
        recordSuiteResult(
            "UNSUPPORTED",
            unsupported,
            null);

        suiteIndex++;

        continue;
      }

      applyEndpointToControls(
          activeSuiteScenario.initial);

      state =
          BenchmarkState.IDLE;

      startRun(
          true);

      return;
    }

    if (suiteActive) {
      finishSuite();
    }
  }

  String unsupportedReason(
      BenchmarkSuitePlan.Scenario scenario) {

    String initial =
        unsupportedOutput(
            scenario.initial);

    if (initial != null) {
      return "Initial endpoint: "
          + initial;
    }

    if (scenario.kind
        == BenchmarkSuitePlan.Kind.TRANSITION) {

      String target =
          unsupportedOutput(
              scenario.target);

      if (target != null) {
        return "Target endpoint: "
            + target;
      }
    }

    return null;
  }

  String unsupportedOutput(
      BenchmarkSuitePlan.Endpoint endpoint) {

    if (endpoint.ndi
        && outputs.getOutputState(
                OutputManager.OutputType.NDI)
            == OutputManager.OutputState.UNAVAILABLE) {

      return "NDI is unavailable";
    }

    if (endpoint.syphon
        && outputs.getOutputState(
                OutputManager.OutputType.SYPHON)
            == OutputManager.OutputState.UNAVAILABLE) {

      return "Syphon is unavailable";
    }

    if (endpoint.spout
        && outputs.getOutputState(
                OutputManager.OutputType.SPOUT)
            == OutputManager.OutputState.UNAVAILABLE) {

      return "Spout is unavailable";
    }

    return null;
  }

  void applyEndpointToControls(
      BenchmarkSuitePlan.Endpoint endpoint) {

    renderModeDropdown.setValue(
        renderModeIndex(
            endpoint.renderMode));

    resolutionDropdown.setValue(
        resolutionIndex(
            endpoint.resolution));

    sceneDropdown.setValue(
        sceneIndex(
            endpoint.scene));

    previewToggle.setState(
        endpoint.preview);

    ndiToggle.setState(
        endpoint.ndi);

    syphonToggle.setState(
        endpoint.syphon);

    spoutToggle.setState(
        endpoint.spout);
  }

  boolean applyTransitionTarget(
      BenchmarkSuitePlan.Endpoint endpoint) {

    applyEndpointToControls(
        endpoint);

    RenderMode mode =
        RenderMode.valueOf(
            endpoint.renderMode);

    ViewType view =
        viewFor(mode);

    dome.setRenderMode(mode);
    dome.setCurrentView(view);

    dome.setCurrentScene(
        sceneOptions[
            sceneIndex(
                endpoint.scene)]);

    dome.setShowPreview(
        endpoint.preview);

    dome.resetGraphics(
        endpoint.resolution);

    outputs.setNdiView(view);
    outputs.setSyphonView(view);
    outputs.setSpoutView(view);

    boolean outputsApplied =
        applyOutput(
            OutputManager.OutputType.NDI,
            endpoint.ndi,
            "ndi")
        && applyOutput(
            OutputManager.OutputType.SYPHON,
            endpoint.syphon,
            "syphon")
        && applyOutput(
            OutputManager.OutputType.SPOUT,
            endpoint.spout,
            "spout");

    applyBenchmarkOutputDemand(
        view,
        outputsApplied && endpoint.outputDemand);

    return outputsApplied;
  }

  void populateTransitionStatistics() {
    if (currentRun == null
        || currentRun.transition == null
        || lastSnapshot == null) {

      return;
    }

    if (transitionFrameIndex <= 0) {
      currentRun.transition = null;
      return;
    }

    int stored =
        lastSnapshot.getStoredFrames();

    if (stored < 2) {
      currentRun.status =
          "FAILED";

      measurementReason =
          "Transition completed without enough frame samples";

      return;
    }

    int marker =
        constrain(
            transitionFrameIndex,
            1,
            stored - 1);

    long[] frameNanos =
        new long[stored];

    for (int index = 0;
         index < stored;
         index++) {

      frameNanos[index] =
          lastSnapshot.getDurationNanos(
              PerformanceMetric.FRAME_TOTAL,
              index);
    }

    BenchmarkTransitionAnalyzer.Statistics statistics =
        BenchmarkTransitionAnalyzer.analyze(
            frameNanos,
            marker);

    currentRun.transition.transitionFrame =
        marker;

    currentRun.transition.baselineFrames =
        statistics.baselineFrames;

    currentRun.transition.postFrames =
        statistics.postFrames;

    currentRun.transition.normalP95Milliseconds =
        statistics.normalP95Milliseconds;

    currentRun.transition.transitionMaximumMilliseconds =
        statistics.transitionMaximumMilliseconds;

    currentRun.transition.recoveryFrames =
        statistics.recoveryFrames;
  }

  void recordSuiteResult(
      String status,
      String reason,
      Path directory) {

    if (suiteSession == null
        || activeSuiteScenario == null) {

      return;
    }

    BenchmarkSuiteResultWriter.Entry entry =
        new BenchmarkSuiteResultWriter.Entry();

    entry.name =
        activeSuiteScenario.name;

    entry.testType =
        activeSuiteScenario.kind.name();

    entry.status =
        status;

    entry.reason =
        reason == null
            ? ""
            : reason;

    entry.resultDirectory =
        directory == null
            ? ""
            : directory.toString();

    suiteSession.scenarios.add(
        entry);
  }

  void finishSuite() {
    suiteActive = false;
    clearBenchmarkOutputDemand();
    activeSuiteScenario = null;

    state =
        BenchmarkState.COMPLETE;

    suiteSession.completedAt =
        Instant.now();

    try {
      lastExportDirectory =
          BenchmarkSuiteResultWriter.export(
              outputRoot,
              suiteSession);

      resultPathLabel.setText(
          "Suite manifest:\n"
          + lastExportDirectory.toString());

      refreshStaticLabels(
          "SUITE COMPLETE - "
          + suiteSession.scenarios.size()
          + " scenario result(s) recorded");

    } catch (IOException error) {
      state =
          BenchmarkState.FAILED;

      refreshStaticLabels(
          "SUITE MANIFEST FAILED - "
          + error.getMessage());
    }

    if (exitAfterSuite) {
      app.exit();
    }
  }

  int renderModeIndex(
      String name) {

    for (int index = 0;
         index < renderModes.length;
         index++) {

      if (renderModes[index]
          .name()
          .equals(name)) {

        return index;
      }
    }

    throw new IllegalArgumentException(
        "Unknown render mode in suite: "
        + name);
  }

  int resolutionIndex(
      int resolution) {

    for (int index = 0;
         index < resolutions.length;
         index++) {

      if (resolutions[index]
          == resolution) {

        return index;
      }
    }

    throw new IllegalArgumentException(
        "Resolution is not available in BenchmarkTool: "
        + resolution);
  }

  int sceneIndex(
      String name) {

    for (int index = 0;
         index < sceneOptions.length;
         index++) {

      if (sceneOptions[index]
          .getName()
          .equals(name)) {

        return index;
      }
    }

    throw new IllegalArgumentException(
        "Scene is not available in BenchmarkTool: "
        + name);
  }

  boolean exportLastRun() {
    if (lastSnapshot == null
        || lastRun == null) {

      refreshStaticLabels(
          "Nothing to export; complete or stop a measurement first");

      return false;
    }

    try {
      lastExportDirectory =
          BenchmarkResultWriter.export(
              outputRoot,
              lastRun,
              lastSnapshot);

      resultPathLabel.setText(
          "Last export:\n"
          + lastExportDirectory.toString());

      refreshStaticLabels(
          "EXPORTED - summary.json, frames.csv, environment.json");

      return true;

    } catch (IOException error) {
      state =
          BenchmarkState.FAILED;

      suiteActive = false;

      refreshStaticLabels(
          "EXPORT FAILED - "
          + error.getMessage());

      return false;
    }
  }

  BenchmarkResultWriter.Run createRunMetadata(
      RenderMode mode,
      ViewType view,
      int resolution,
      Scene scene) {

    BenchmarkResultWriter.Run run =
        new BenchmarkResultWriter.Run();

    run.timestamp =
        Instant.now();

    run.libraryVersion =
        "2.0.0";

    run.revision =
        configuredRevision();

    run.renderMode =
        mode.name();

    run.view =
        view.name();

    run.resolution =
        resolution;

    run.resolutionDomain =
        benchmarkOutputDemandActive
                || outputs.isActive()
            ? "OUTPUT_BASE"
            : (
                mode == RenderMode.STANDARD
                    ? "PREVIEW_WINDOW"
                    : "PREVIEW_CUBEMAP"
            );

    run.outputDemand =
        benchmarkOutputDemandActive;

    run.benchmarkFpsTarget =
        configuredBenchmarkFps;

    run.scene =
        scene.getName();

    run.preview =
        previewToggle.getState();

    run.ndiRequested =
        ndiToggle.getState();

    run.syphonRequested =
        syphonToggle.getState();

    run.spoutRequested =
        spoutToggle.getState();

    run.ndiStatus =
        run.ndiRequested
            ? "SUPPORTED"
            : "NOT_TESTED";

    run.syphonStatus =
        run.syphonRequested
            ? "SUPPORTED"
            : "NOT_TESTED";

    run.spoutStatus =
        run.spoutRequested
            ? "SUPPORTED"
            : "NOT_TESTED";

    run.warmupFrames =
        configuredWarmupFrames;

    run.requestedMeasurementFrames =
        configuredMeasurementFrames;

    refreshEnvironment(
        run);

    return run;
  }

  void refreshEnvironment(
      BenchmarkResultWriter.Run run) {

    BenchmarkResultWriter.Environment environment =
        run.environment;

    environment.os =
        known(
            System.getProperty(
                "os.name"));

    environment.osVersion =
        known(
            System.getProperty(
                "os.version"));

    environment.architecture =
        known(
            System.getProperty(
                "os.arch"));

    environment.javaVersion =
        known(
            System.getProperty(
                "java.version"));

    environment.processingVersion =
        processingVersion();

    ProcessingGlCapabilities capabilities =
        ProcessingGlAdapter
            .getDefault()
            .queryCapabilities(app);

    environment.glVendor =
        known(
            capabilities.vendor());

    environment.glRenderer =
        known(
            capabilities.renderer());

    environment.glVersion =
        known(
            capabilities.version());

    environment.glslVersion =
        known(
            capabilities.shadingLanguageVersion());

    environment.joglProfile =
        known(
            capabilities.joglProfile());

    environment.hardwareRasterizerKnown =
        capabilities.isHardwareRasterizerKnown();

    environment.hardwareRasterizer =
        capabilities.isHardwareRasterizer();

    environment.windowWidth =
        app.width;

    environment.windowHeight =
        app.height;

    environment.pixelDensity =
        app.pixelDensity;

    environment.ndiState =
        outputs.getOutputState(
                OutputManager.OutputType.NDI)
            .name();

    environment.syphonState =
        outputs.getOutputState(
                OutputManager.OutputType.SYPHON)
            .name();

    environment.spoutState =
        outputs.getOutputState(
                OutputManager.OutputType.SPOUT)
            .name();
  }

  void captureNdiBaseline() {
    ndiCapturedBefore =
        outputs.getNdiCapturedFrames();

    ndiSentBefore =
        outputs.getNdiSentFrames();

    ndiDroppedBefore =
        outputs.getNdiDroppedFrames();

    ndiFailedBefore =
        outputs.getNdiFailedFrames();
  }

  void captureNdiDelta(
      BenchmarkResultWriter.Run run) {

    run.ndiCaptured =
        Math.max(
            0L,
            outputs.getNdiCapturedFrames()
                - ndiCapturedBefore);

    run.ndiSent =
        Math.max(
            0L,
            outputs.getNdiSentFrames()
                - ndiSentBefore);

    run.ndiDropped =
        Math.max(
            0L,
            outputs.getNdiDroppedFrames()
                - ndiDroppedBefore);

    run.ndiFailed =
        Math.max(
            0L,
            outputs.getNdiFailedFrames()
                - ndiFailedBefore);
  }

  void updateResultViews() {
    PerformanceSnapshot.MetricStatistics frame =
        lastSnapshot.getStatistics(
            PerformanceMetric.FRAME_TOTAL);

    liveLabel.setText(
        "Frames: "
        + lastSnapshot.getStoredFrames()
        + "   Avg: "
        + nf(
            (float) frame.getAverageMilliseconds(),
            0,
            3)
        + " ms"
        + "   FPS: "
        + nf(
            (float) frame.getAverageFps(),
            0,
            2)
        + "\n"
        + "P50: "
        + nf(
            (float) frame.getP50Milliseconds(),
            0,
            3)
        + "   P95: "
        + nf(
            (float) frame.getP95Milliseconds(),
            0,
            3)
        + "   P99: "
        + nf(
            (float) frame.getP99Milliseconds(),
            0,
            3)
        + "   Max: "
        + nf(
            (float) frame.getMaximumMilliseconds(),
            0,
            3)
        + " ms\n"
        + "1% low: "
        + nf(
            (float) frame.getOnePercentLowFps(),
            0,
            2)
        + "   >16.67: "
        + frame.getFramesOver16Point67Milliseconds()
        + "   >33.33: "
        + frame.getFramesOver33Point33Milliseconds()
        + "   >50: "
        + frame.getFramesOver50Milliseconds());

    pipelineLabel.setText(
        gpuPipelineLine()
        + pipelineLine(
            "Standard",
            PerformanceMetric.STANDARD_RENDER)
        + pipelineLine(
            "Cubemap",
            PerformanceMetric.CUBEMAP_TOTAL)
        + pipelineLine(
            "Domemaster",
            PerformanceMetric.DOMEMASTER)
        + pipelineLine(
            "Equirect",
            PerformanceMetric.EQUIRECTANGULAR)
        + pipelineLine(
            "Skybox",
            PerformanceMetric.SKYBOX)
        + pipelineLine(
            "Preview",
            PerformanceMetric.PREVIEW_PIPELINE)
        + pipelineLine(
            "NDI capture",
            PerformanceMetric.NDI_CAPTURE)
        + "Invariants: "
        + lastSnapshot.getInvariantViolations());

    int retained =
        lastSnapshot.getStoredFrames();

    int chartSamples =
        min(
            300,
            retained);

    float[] history =
        new float[
            max(
                1,
                chartSamples)];

    float maximum =
        16.67f;

    for (int index = 0;
         index < chartSamples;
         index++) {

      int source =
          retained
          - chartSamples
          + index;

      history[index] =
          (float) (
              lastSnapshot.getDurationNanos(
                  PerformanceMetric.FRAME_TOTAL,
                  source)
              / 1000000.0);

      maximum =
          max(
              maximum,
              history[index]);
    }

    frameChart.setRange(
        0,
        max(
            20.0f,
            maximum * 1.1f));

    frameChart.setData(
        "frameMs",
        history);

    refreshOutputLabel();

    refreshStaticLabels(
        "COMPLETE - inspect results, then press EXPORT");
  }

  String pipelineLine(
      String label,
      PerformanceMetric metric) {

    PerformanceSnapshot.MetricStatistics statistics =
        lastSnapshot.getStatistics(
            metric);

    return label
        + ": avg "
        + nf(
            (float) statistics.getAverageMilliseconds(),
            0,
            3)
        + "  p95 "
        + nf(
            (float) statistics.getP95Milliseconds(),
            0,
            3)
        + "  calls/f "
        + nf(
            (float) statistics.getAverageCallsPerFrame(),
            0,
            2)
        + "\n";
  }

  String gpuPipelineLine() {
    PerformanceSnapshot.MetricStatistics statistics =
        lastSnapshot.getGpuStatistics(
            PerformanceMetric.RENDER_PIPELINE);

    return "GPU pipeline ["
        + lastSnapshot.getEffectiveMode().name()
        + "/"
        + lastSnapshot.getGpuTimerBackend().name()
        + "/"
        + lastSnapshot.getGpuTimerArchitecture().name()
        + "]: avg "
        + nf(
            (float) statistics.getAverageMilliseconds(),
            0,
            3)
        + "  p95 "
        + nf(
            (float) statistics.getP95Milliseconds(),
            0,
            3)
        + "  samples "
        + statistics.getSampledFrames()
        + "\n";
  }

  PerformanceMode selectedPerformanceMode() {
    return gpuToggle.getState()
        ? PerformanceMode.CPU_GPU
        : PerformanceMode.CPU;
  }

  boolean gpuProfilingDefault() {
    String configured =
        System.getenv(
            "ZIVIDOME_BENCHMARK_GPU");

    return configured != null
        && configured.equalsIgnoreCase(
            "true");
  }

  void refreshStaticLabels(
      String message) {

    systemLabel.setText(
        "State: "
        + state.name()
        + "   Java "
        + known(
            System.getProperty(
                "java.version"))
        + "   Processing "
        + processingVersion()
        + "\n"
        + message);

    if (state
        != BenchmarkState.COMPLETE) {

      liveLabel.setText(
          "No snapshot aggregation during measurement.\n"
          + message);
    }

    refreshOutputLabel();

    resultPathLabel.setText(
        "Output root:\n"
        + outputRoot.toString());
  }

  void refreshOutputLabel() {
    outputLabel.setText(
        "NDI: "
        + outputs.getOutputState(
            OutputManager.OutputType.NDI)
        + "  captured/sent/dropped/failed: "
        + outputs.getNdiCapturedFrames()
        + "/"
        + outputs.getNdiSentFrames()
        + "/"
        + outputs.getNdiDroppedFrames()
        + "/"
        + outputs.getNdiFailedFrames()
        + "\n"
        + "Syphon: "
        + outputs.getOutputState(
            OutputManager.OutputType.SYPHON)
        + "   Spout: "
        + outputs.getOutputState(
            OutputManager.OutputType.SPOUT));
  }

  void updateOutputLocks() {
    if (outputs.getOutputState(
            OutputManager.OutputType.NDI)
        == OutputManager.OutputState.UNAVAILABLE) {

      ndiToggle
          .setState(false)
          .setLock(true);
    }

    if (outputs.getOutputState(
            OutputManager.OutputType.SYPHON)
        == OutputManager.OutputState.UNAVAILABLE) {

      syphonToggle
          .setState(false)
          .setLock(true);
    }

    if (outputs.getOutputState(
            OutputManager.OutputType.SPOUT)
        == OutputManager.OutputState.UNAVAILABLE) {

      spoutToggle
          .setState(false)
          .setLock(true);
    }
  }

  int sampleCapacity() {
    int required =
        configuredMeasurementFrames;

    if (suiteActive
        && activeSuiteScenario != null
        && activeSuiteScenario.kind
            == BenchmarkSuitePlan.Kind.TRANSITION) {

      required =
          activeSuiteScenario.baselineFrames
          + activeSuiteScenario.postFrames;
    }

    return min(
        100000,
        max(
            4096,
            required + 8));
  }

  boolean isRunning() {
    return state == BenchmarkState.WARMUP
        || state == BenchmarkState.WARMUP_FINISHING
        || state == BenchmarkState.MEASURING
        || state == BenchmarkState.TRANSITION_BASELINE
        || state == BenchmarkState.TRANSITION_APPLYING
        || state == BenchmarkState.TRANSITION_RECOVERY
        || state == BenchmarkState.MEASUREMENT_FINISHING;
  }

  RenderMode selectedRenderMode() {
    int index =
        constrain(
            round(
                renderModeDropdown.getValue()),
            0,
            renderModes.length - 1);

    return renderModes[index];
  }

  int selectedResolution() {
    int index =
        constrain(
            round(
                resolutionDropdown.getValue()),
            0,
            resolutions.length - 1);

    return resolutions[index];
  }

  int selectedSceneIndex() {
    return constrain(
        round(
            sceneDropdown.getValue()),
        0,
        sceneOptions.length - 1);
  }

  ViewType viewFor(
      RenderMode mode) {

    switch (mode) {
      case STANDARD:
        return ViewType.STANDARD;

      case EQUIRECTANGULAR:
        return ViewType.EQUIRECTANGULAR;

      case SKYBOX:
        return ViewType.SKYBOX;

      case DOMEMASTER:
      default:
        return ViewType.DOMEMASTER;
    }
  }

  void applyBenchmarkOutputDemand(
      ViewType view,
      boolean enabled) {

    benchmarkOutputDemandActive =
        enabled;

    dome.setPerformanceOutputDemand(
        enabled
            ? view
            : null);
  }

  void clearBenchmarkOutputDemand() {
    benchmarkOutputDemandActive = false;
    dome.setPerformanceOutputDemand(null);
  }

  boolean outputEnabled(
      OutputManager.OutputType type) {

    switch (type) {
      case NDI:
        return outputs.isNdiEnabled();

      case SYPHON:
        return outputs.isSyphonEnabled();

      case SPOUT:
        return outputs.isSpoutEnabled();

      default:
        return false;
    }
  }

  Path resolveOutputRoot() {
    String configured =
        firstKnown(
            System.getenv(
                "ZIVIDOME_BENCHMARK_OUTPUT"),
            System.getProperty(
                "zividome.benchmark.output"));

    if (!configured.equals("unknown")) {
      return Paths.get(
          configured);
    }

    return Paths.get(
        System.getProperty(
            "user.home"),
        "ziviDomeLive-benchmark-results");
  }

  void configureAutomation() {
    automationSuiteName =
        configuredValue(
            "ZIVIDOME_BENCHMARK_SUITE",
            "zividome.benchmark.suite");

    automationSceneName =
        configuredValue(
            "ZIVIDOME_BENCHMARK_SCENE",
            "zividome.benchmark.scene");

    exitAfterSuite =
        configuredBoolean(
            "ZIVIDOME_BENCHMARK_EXIT",
            "zividome.benchmark.exit",
            false);

    configuredGpuTimerPolicy =
        configuredGpuTimerPolicy();

    configuredBenchmarkNdi =
        configuredBoolean(
            "ZIVIDOME_BENCHMARK_NDI",
            "zividome.benchmark.ndi",
            false);

    configuredBenchmarkFps =
        configuredInteger(
            "ZIVIDOME_BENCHMARK_FPS",
            "zividome.benchmark.fps",
            configuredBenchmarkFps,
            1,
            1000);

    app.frameRate(
        configuredBenchmarkFps);

    configuredWarmupFrames =
        configuredInteger(
            "ZIVIDOME_BENCHMARK_WARMUP_FRAMES",
            "zividome.benchmark.warmupFrames",
            configuredWarmupFrames,
            0,
            10000);

    configuredMeasurementFrames =
        configuredInteger(
            "ZIVIDOME_BENCHMARK_MEASUREMENT_FRAMES",
            "zividome.benchmark.measurementFrames",
            configuredMeasurementFrames,
            2,
            20000);

    configuredTransitionBaselineFrames =
        configuredInteger(
            "ZIVIDOME_BENCHMARK_TRANSITION_BASELINE_FRAMES",
            "zividome.benchmark.transitionBaselineFrames",
            configuredTransitionBaselineFrames,
            2,
            10000);

    configuredTransitionPostFrames =
        configuredInteger(
            "ZIVIDOME_BENCHMARK_TRANSITION_POST_FRAMES",
            "zividome.benchmark.transitionPostFrames",
            configuredTransitionPostFrames,
            2,
            20000);

    int automatedResolution =
        configuredInteger(
            "ZIVIDOME_BENCHMARK_RESOLUTION",
            "zividome.benchmark.resolution",
            selectedResolution(),
            1,
            16384);

    resolutionDropdown.setValue(
        resolutionIndex(
            automatedResolution));

    previewToggle.setState(
        configuredBoolean(
            "ZIVIDOME_BENCHMARK_PREVIEW",
            "zividome.benchmark.preview",
            previewToggle.getState()));

    ndiToggle.setState(
        configuredBenchmarkNdi);

    warmupFramesInput.setValue(
        configuredWarmupFrames);

    measurementFramesInput.setValue(
        configuredMeasurementFrames);
  }

  GpuTimerPolicy configuredGpuTimerPolicy() {
    String configured =
        configuredValue(
            "ZIVIDOME_BENCHMARK_GPU_TIMER_POLICY",
            "zividome.benchmark.gpuTimerPolicy");

    if (configured == null
        || configured.equalsIgnoreCase(
            "AUTO")) {

      return GpuTimerPolicy.ARCHITECTURE_AWARE;
    }

    if (configured.equalsIgnoreCase(
        "TIMESTAMP")) {

      return GpuTimerPolicy.SAFE;
    }

    if (configured.equalsIgnoreCase(
        "ELAPSED")) {

      return GpuTimerPolicy.TIME_ELAPSED_EXCLUSIVE;
    }

    try {
      return GpuTimerPolicy.valueOf(
          configured.toUpperCase(
              Locale.ROOT));

    } catch (IllegalArgumentException error) {
      throw new IllegalArgumentException(
          "ZIVIDOME_BENCHMARK_GPU_TIMER_POLICY must be AUTO, SAFE, TIMESTAMP, "
          + "ARCHITECTURE_AWARE, ELAPSED, or TIME_ELAPSED_EXCLUSIVE.");
    }
  }

  int configuredInteger(
      String environmentName,
      String propertyName,
      int fallback,
      int minimum,
      int maximum) {

    String configured =
        configuredValue(
            environmentName,
            propertyName);

    if (configured == null) {
      return fallback;
    }

    try {
      int value =
          Integer.parseInt(
              configured);

      if (value < minimum
          || value > maximum) {

        throw new NumberFormatException();
      }

      return value;

    } catch (NumberFormatException error) {
      throw new IllegalArgumentException(
          environmentName
          + " must be an integer from "
          + minimum
          + " to "
          + maximum
          + ".");
    }
  }

  boolean configuredBoolean(
      String environmentName,
      String propertyName,
      boolean fallback) {

    String configured =
        configuredValue(
            environmentName,
            propertyName);

    if (configured == null) {
      return fallback;
    }

    if (configured.equalsIgnoreCase(
        "true")) {

      return true;
    }

    if (configured.equalsIgnoreCase(
        "false")) {

      return false;
    }

    throw new IllegalArgumentException(
        environmentName
        + " must be true or false.");
  }

  String configuredValue(
      String environmentName,
      String propertyName) {

    String environment =
        System.getenv(
            environmentName);

    if (environment != null
        && !environment.trim().isEmpty()) {

      return environment.trim();
    }

    String property =
        System.getProperty(
            propertyName);

    return property == null
            || property.trim().isEmpty()
        ? null
        : property.trim();
  }

  String configuredRevision() {
    return firstKnown(
        System.getenv(
            "ZIVIDOME_BENCHMARK_REVISION"),
        System.getenv(
            "GITHUB_SHA"),
        System.getProperty(
            "zividome.benchmark.revision"));
  }

  String firstKnown(
      String... candidates) {

    for (String candidate : candidates) {
      if (candidate != null
          && !candidate.trim().isEmpty()) {

        return candidate.trim();
      }
    }

    return "unknown";
  }

  String known(
      String value) {

    return value == null
            || value.trim().isEmpty()
        ? "unknown"
        : value;
  }

  String processingVersion() {
    String configured =
        System.getProperty(
            "processing.version");

    if (configured != null
        && !configured.trim().isEmpty()) {

      return configured.trim();
    }

    try {
      String implementation =
          PApplet.class
              .getPackage()
              .getImplementationVersion();

      if (implementation != null
          && !implementation.trim().isEmpty()) {

        return implementation.trim();
      }

      String location =
          PApplet.class
              .getProtectionDomain()
              .getCodeSource()
              .getLocation()
              .toString();

      int marker =
          location.lastIndexOf(
              "core-");

      int suffix =
          marker >= 0
              ? location.indexOf(
                  ".jar",
                  marker)
              : -1;

      if (marker >= 0
          && suffix > marker + 5) {

        return location.substring(
            marker + 5,
            suffix);
      }

    } catch (RuntimeException ignored) {
      // A restricted runtime may hide code-source metadata.
    }

    return "unknown";
  }
}