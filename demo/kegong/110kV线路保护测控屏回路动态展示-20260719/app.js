const operationLoops = [
  {
    row: 1, name: "操作电源", tag: "POWER", group: "all",
    summary: "直流操作电源经 1-4QD 端子送入操作回路，为监视、合闸、跳闸及闭锁支路提供电源。",
    components: ["-1-4QD:1", "1-4QD:2", "-1-4QD:18", "1-4QD:19"],
    paths: ["M44 279 L44 264 L150 264", "M352 279 L352 264 L286 264"]
  },
  {
    row: 2, name: "跳位继电器 / 跳位灯", tag: "TWJ", group: "close",
    summary: "断路器分位时，TWJ 与跳位指示支路具备导通条件，用于分位状态监视。",
    components: ["TWJ", "R", "LED", "31 / 28", "1-4CD:4"],
    relay: { code: "TWJ", name: "跳位继电器", target: [178, 256], action: "得电吸合", principle: "断路器分位时线圈得电，触点切换并点亮跳位灯，同时经 HQ 合闸线圈监视合闸回路完整性。" },
    lamp: { code: "跳位灯", target: [244, 264] },
    requires: state => state.breaker === "open",
    conditionText: "需要断路器处于分位",
    paths: ["M44 279 L44 264 L164 264 L164 256 L282 256 L304 256 L314 256 L314 244 L336 244 L336 264 L352 264 L352 279"]
  },
  {
    row: 3, name: "合保持及合闸线圈", tag: "HBJ · HQ", group: "close",
    summary: "合闸命令经 HBJ、TBJV 与机构辅助接点送至 HQ 合闸线圈；保持支路维持命令直至动作完成。",
    components: ["HBJ", "TBJV", "1-4CD:5/6", "DL", "HQ"],
    relay: { code: "HBJ", name: "合闸保持继电器", target: [213, 244], action: "吸合并保持", principle: "合闸命令使 HBJ 动作，其保持触点维持合闸电流，直至断路器完成合闸。" },
    requires: state => state.breaker === "open",
    conditionText: "合闸操作要求断路器处于分位",
    paths: ["M44 279 L44 264 L164 264 L164 244 L282 244 L304 244 L336 244 L336 264 L352 264 L352 279"]
  },
  {
    row: 4, name: "防跳", tag: "TBJ · TBJV", group: "lock",
    summary: "合闸命令持续存在时，TBJ/TBJV 防止断路器在故障跳闸后反复合跳。JW7 决定外部压力接点是否串入。",
    components: ["TBJ", "TBJV", "BSH", "JW7"],
    relay: { code: "TBJV", name: "防跳继电器", target: [241, 220], action: "触点切换", principle: "跳闸后若合闸命令仍存在，防跳继电器改变触点状态，阻断再次合闸，避免反复跳合。" },
    requires: state => state.antiPump === "device" && state.jw7,
    conditionText: "装置防跳模式且 JW7 接通",
    paths: ["M44 279 L44 264 L164 264 L164 244 L164 236 L200 236 L200 228 L232 228 L232 220 L272 220 L272 264 L352 264 L352 279", "M168 236 L168 228 L188 228 L188 220 L164 220"]
  },
  {
    row: 5, name: "保护合", tag: "TBJ · TBJV", group: "close",
    summary: "保护合闸电流由 1-4QD:15 经端子 10 向右流过 TBJ 接点和 TBJV 线圈，再经端子 31 返回 1-4QD:19；不反向经过 SHJ 接点。",
    components: ["1-4QD:15", "10", "TBJ", "TBJV", "31", "1-4QD:19"],
    relay: { code: "TBJV", name: "防跳继电器", target: [244, 220], action: "得电吸合", principle: "保护合闸命令经 TBJ 接点后使 TBJV 线圈得电；TBJV 触点随即切换，参与防止持续合闸命令造成反复跳合。" },
    requires: state => state.breaker === "open",
    conditionText: "保护合闸要求断路器处于分位",
    paths: ["M132 200 L152 200 L200 200 L200 228 L232 228 L232 220 L272 220 L272 264 L284 264 L352 264 L352 279"]
  },
  {
    row: 6, name: "置位", tag: "HHJ", group: "close",
    summary: "合后状态置位支路经电阻与 HHJ 建立记忆，为后续合后逻辑提供状态。",
    components: ["R", "HHJ", "合后记忆"],
    relay: { code: "HHJ", name: "合后继电器", target: [241, 188], action: "置位吸合", principle: "断路器合闸后建立合后记忆，供重合闸闭锁、手跳识别等后续逻辑使用。" },
    paths: ["M132 200 L152 200 L200 200 L200 188 L272 188 L272 264 L352 264 L352 279"]
  },
  {
    row: 7, name: "复位", tag: "HHJ", group: "close",
    summary: "复位支路解除合后记忆，使相关保持逻辑回到初始状态。",
    components: ["R", "HHJ", "复位支路"],
    relay: { code: "HHJ", name: "合后继电器", target: [241, 180], action: "复位释放", principle: "复位信号使 HHJ 解除保持，相关触点返回初始位置，清除合后记忆。" },
    paths: ["M132 168 L152 168 L200 168 L200 180 L272 180 L272 264 L352 264 L352 279"]
  },
  {
    row: 8, name: "手跳 / 遥跳", tag: "STJ", group: "trip",
    summary: "手动或遥控跳闸命令由端子 30 进入，经 STJ 支路送往跳闸逻辑。",
    components: ["1-4QD:11/12", "30", "R", "STJ"],
    relay: { code: "STJ", name: "手跳继电器", target: [229, 168], action: "得电吸合", principle: "手跳或遥跳命令使 STJ 动作，其触点将跳闸命令送往保持和跳闸线圈。" },
    requires: state => state.breaker === "closed",
    conditionText: "跳闸演示要求断路器处于合位",
    paths: ["M132 168 L152 168 L272 168 L272 264 L352 264 L352 279"]
  },
  {
    row: 9, name: "手合 / 遥合", tag: "SHJ", group: "close",
    summary: "手动或遥控合闸命令由端子 16 进入，经 SHJ 支路送往合闸保持与线圈回路。",
    components: ["1-4QD:14", "16", "R", "SHJ"],
    relay: { code: "SHJ", name: "手合继电器", target: [241, 156], action: "得电吸合", principle: "手合或遥合命令使 SHJ 动作，触点闭合后启动合闸保持及 HQ 合闸线圈。" },
    requires: state => state.breaker === "open",
    conditionText: "合闸演示要求断路器处于分位",
    paths: ["M132 156 L152 156 L272 156 L272 264 L352 264 L352 279"]
  },
  {
    row: 10, name: "永跳", tag: "TJR", group: "trip",
    summary: "永跳命令由端子 32 进入，经 TJR 支路直接参与跳闸链路。",
    components: ["1-4QD:9/10", "32", "R", "TJR"],
    relay: { code: "TJR", name: "永跳继电器", target: [253, 144], action: "得电吸合", principle: "永跳命令使 TJR 动作，跳闸后保持禁止再次合闸，需解除相应条件后方可复归。" },
    requires: state => state.breaker === "closed",
    conditionText: "跳闸演示要求断路器处于合位",
    paths: ["M132 144 L152 144 L272 144 L272 264 L352 264 L352 279"]
  },
  {
    row: 11, name: "保护跳", tag: "STJ · TJR", group: "trip",
    summary: "保护跳闸输出由端子 08 进入，经 STJ/TJR 与 BST 支路形成跳闸命令；JW9 用于外部压力闭锁配置。",
    components: ["1-4QD:7/8", "08", "STJ", "TJR", "BST", "JW9"],
    relay: { code: "STJ", name: "保护跳闸继电器", target: [178, 132], action: "触点闭合", principle: "保护出口到达后，STJ/TJR 相关触点切换，将跳闸命令送入 TBJ 保持和 TQ 线圈。" },
    requires: state => state.breaker === "closed" && state.jw9,
    conditionText: "断路器合位且 JW9 接通",
    paths: ["M132 116 L152 116 L196 116 L196 132 L244 132 L244 104 L282 104 L304 104 L314 104 L336 104 L336 264 L352 264 L352 279"]
  },
  {
    row: 12, name: "跳保持及跳闸线圈", tag: "TBJ · TQ", group: "trip",
    summary: "跳闸命令经 TBJ 保持后，通过 1-4CD:1 与机构辅助接点送至 TQ 跳闸线圈。",
    components: ["TBJ", "12", "1-4CD:1", "DL", "TQ"],
    relay: { code: "TBJ", name: "跳闸保持继电器", target: [213, 104], action: "吸合并保持", principle: "跳闸命令使 TBJ 吸合并自保持，保证 TQ 跳闸线圈获得足够的可靠动作时间。" },
    requires: state => state.breaker === "closed",
    conditionText: "跳闸操作要求断路器处于合位",
    paths: ["M44 279 L44 264 L164 264 L164 104 L282 104 L304 104 L336 104 L336 264 L352 264 L352 279"]
  },
  {
    row: 13, name: "合位继电器 / 合位灯", tag: "HWJ", group: "trip",
    summary: "断路器合位时，HWJ 与合位指示支路具备导通条件，用于合位状态监视。",
    components: ["HWJ", "R", "LED", "14", "1-4CD:2"],
    relay: { code: "HWJ", name: "合位继电器", target: [177, 88], action: "得电吸合", principle: "断路器合位后 HWJ 得电，触点切换并点亮合位灯，同时经 TQ 跳闸线圈监视跳闸回路完整性。" },
    lamp: { code: "合位灯", target: [244, 96] },
    requires: state => state.breaker === "closed",
    conditionText: "需要断路器处于合位",
    paths: ["M44 279 L44 264 L164 264 L164 88 L282 88 L304 88 L314 88 L314 104 L336 104 L336 264 L352 264 L352 279"]
  },
  {
    row: 14, name: "闭锁合闸", tag: "BSH", group: "lock",
    summary: "开关机构 BSH 接点经端子 04 和横向二极管进入闭锁合闸支路；二极管仅允许电流由左向右通过，禁止反向串入其他闭锁支路。",
    components: ["BSH", "04", "二极管：左→右", "1-4QD:18"],
    relay: { code: "BSH", name: "闭锁合闸接点", target: [93, 76], action: "接点切换", principle: "机构条件异常时 BSH 接点改变状态，使合闸支路无法导通，防止不满足条件时合闸。" },
    diodes: [{ target: [178, 76], direction: "right" }],
    paths: ["M44 104 L44 76 L154 76 L184 76 L216 76 L272 76 L272 264 L352 264 L352 279"]
  },
  {
    row: 15, name: "总闭锁", tag: "ZBS", group: "lock",
    summary: "总闭锁接点 ZBS 经端子 02 后在实心连接点处分成上下两路，分别按二极管正向馈入 BSH 与 BST；反向电流被截止。",
    components: ["ZBS", "02", "实心点分支", "二极管：向上/向下"],
    relay: { code: "ZBS", name: "总闭锁接点", target: [93, 64], action: "接点切换", principle: "总闭锁条件出现时，ZBS 通过隔离网络同时影响相关操作支路，实现统一闭锁。" },
    diodes: [{ target: [216, 70], direction: "up" }, { target: [216, 58], direction: "down" }],
    paths: ["M44 104 L44 64 L154 64 L216 64 L216 76 L272 76 L272 264 L352 264 L352 279", "M44 104 L44 64 L154 64 L216 64 L216 52 L272 52 L272 264 L352 264 L352 279"]
  },
  {
    row: 16, name: "闭锁跳闸", tag: "BST", group: "lock",
    summary: "闭锁跳闸接点 BST 经端子 06 和横向二极管进入闭锁跳闸支路；二极管仅允许电流由左向右通过。",
    components: ["BST", "06", "二极管：左→右", "1-4QD:18"],
    relay: { code: "BST", name: "闭锁跳闸接点", target: [93, 52], action: "接点切换", principle: "闭锁条件出现时 BST 改变状态，经二极管隔离后阻断指定跳闸路径。" },
    diodes: [{ target: [178, 52], direction: "right" }],
    paths: ["M44 104 L44 52 L154 52 L184 52 L216 52 L272 52 L272 264 L352 264 L352 279"]
  }
];

const drawingPages = [
  ...additionalDrawingPages,
  {
    id: "31-15", title: "1-1n 操作回路图", shortTitle: "操作回路", entityCount: 1193,
    note: "1-1n 装置操作、跳合闸监视及闭锁回路。",
    filters: [{ id: "close", label: "合闸" }, { id: "trip", label: "跳闸" }, { id: "lock", label: "闭锁" }],
    loops: operationLoops
  }
].sort((a, b) => a.id.localeCompare(b.id));

let activePage = drawingPages.find(page => page.id === "31-15");
let loops = activePage.loops;
const state = { breaker: "open", antiPump: "device", jw7: true, jw9: true };
const deviceStates = {};
let activeIndex = 0;
let activeFilter = "all";
let autoplayTimer = null;
let relayTimer = null;
let currentRelay = null;
let currentViewBox = { x: 0, y: 0, w: 420, h: 297 };
let dragging = null;

const elements = {
  list: document.getElementById("loopList"),
  count: document.getElementById("loopCount"),
  overlay: document.getElementById("overlayRoot"),
  lampOverlay: document.getElementById("lampOverlay"),
  deviceOverlay: document.getElementById("deviceOverlay"),
  relay: document.getElementById("relayOverlay"),
  relayPanel: document.getElementById("relaySidePanel"),
  relayDirection: document.getElementById("relaySideDirection"),
  relayCode: document.getElementById("relaySideCode"),
  relayName: document.getElementById("relaySideName"),
  relayAction: document.getElementById("relaySideAction"),
  relayPrinciple: document.getElementById("relaySidePrinciple"),
  guide: document.getElementById("relayGuide"),
  guidePath: document.getElementById("relayGuidePath"),
  guideTail: document.getElementById("relayGuideTail"),
  guideLabel: document.getElementById("relayGuideLabel"),
  svg: document.getElementById("diagramSvg"),
  viewport: document.getElementById("diagramViewport"),
  index: document.getElementById("activeIndex"),
  title: document.getElementById("activeTitle"),
  summary: document.getElementById("activeSummary"),
  status: document.getElementById("activeStatus"),
  chips: document.getElementById("componentChips"),
  play: document.getElementById("playBtn"),
  next: document.getElementById("nextBtn"),
  speed: document.getElementById("speedSelect")
};
elements.pageHeading = document.getElementById("pageHeading");
elements.entityCount = document.getElementById("entityCount");
elements.drawingSelect = document.getElementById("drawingSelect");
elements.drawingNote = document.getElementById("drawingNote");
elements.filterTabs = document.getElementById("filterTabs");
elements.drawingImage = document.getElementById("drawingImage");
elements.diagramTitle = document.getElementById("diagramTitle");
elements.diagramDesc = document.getElementById("diagramDesc");
elements.stateCard = document.getElementById("stateCard");
elements.simulationControls = document.getElementById("simulationControls");

function renderFilters() {
  elements.filterTabs.style.gridTemplateColumns = `repeat(${activePage.filters.length + 1}, 1fr)`;
  elements.filterTabs.innerHTML = [
    { id: "all", label: "全部" },
    ...activePage.filters
  ].map((filter, index) => `<button class="filter${index === 0 ? " active" : ""}" data-filter="${filter.id}" role="tab">${filter.label}</button>`).join("");

  elements.filterTabs.querySelectorAll(".filter").forEach(button => button.addEventListener("click", () => {
    activeFilter = button.dataset.filter;
    elements.filterTabs.querySelectorAll(".filter").forEach(item => item.classList.toggle("active", item === button));
    const activeLoop = loops[activeIndex];
    if (activeFilter !== "all" && activeLoop.group !== activeFilter) {
      selectLoop(loops.findIndex(loop => loop.group === activeFilter));
    } else {
      renderList();
    }
  }));
}

function selectPage(pageId) {
  stopAutoplay();
  activePage = drawingPages.find(page => page.id === pageId) || drawingPages[0];
  loops = activePage.loops;
  activeIndex = 0;
  activeFilter = "all";
  currentViewBox = { x: 0, y: 0, w: 420, h: 297 };
  elements.svg.setAttribute("viewBox", "0 0 420 297");
  elements.drawingImage.setAttribute("href", `assets/${activePage.id}-circuit.png`);
  elements.pageHeading.textContent = `${activePage.id} ${activePage.shortTitle}动态教学`;
  elements.entityCount.textContent = `${activePage.entityCount} 个实体`;
  elements.drawingNote.textContent = activePage.note;
  elements.diagramTitle.textContent = `${activePage.id} ${activePage.title}`;
  elements.diagramDesc.textContent = `${activePage.note} 由原始 DWG 对应 PDF 页生成完整图纸，并叠加可交互电流路径。`;
  document.title = `${activePage.id} ${activePage.shortTitle}动态教学`;
  elements.drawingSelect.value = activePage.id;
  renderFilters();
  elements.list.setAttribute("aria-label", `${loops.length} 个功能回路`);
  selectLoop(0, { keepAutoplay: true });
}

function isOperationReady(loop) {
  return loop.requires ? loop.requires(state) : true;
}

function isReady(loop) {
  const operationReady = isOperationReady(loop);
  const deviceReady = loop.device ? getDeviceState(loop.device) : true;
  return operationReady && deviceReady;
}

function getDeviceState(device) {
  const pageStates = deviceStates[activePage.id] ||= {};
  if (!(device.id in pageStates)) pageStates[device.id] = device.defaultState !== false;
  return pageStates[device.id];
}

function setDeviceState(device, value) {
  const pageStates = deviceStates[activePage.id] ||= {};
  pageStates[device.id] = value;
  selectLoop(activeIndex, { keepAutoplay: true });
}

function deviceControlCopy(device) {
  if (device.type === "button") return { kind: "按钮", off: "释放", on: "按下" };
  if (device.type === "plate") return { kind: "压板", off: "退出", on: "投入" };
  if (device.type === "switch") return { kind: "开关", off: "分", on: "合" };
  return { kind: "空开", off: "分", on: "合" };
}

function renderSimulationControls(loop) {
  elements.stateCard.hidden = false;
  if (activePage.id === "31-15") {
    elements.simulationControls.innerHTML = `
      <div class="toolbar-state-group">
        <span>断路器</span>
        <div class="segmented toolbar-segmented" role="group" aria-label="断路器状态">
          <button class="${state.breaker === "open" ? "active" : ""}" data-state="breaker" data-value="open">分位</button>
          <button class="${state.breaker === "closed" ? "active" : ""}" data-state="breaker" data-value="closed">合位</button>
        </div>
        <span id="breakerLamp" class="breaker-lamp ${state.breaker === "closed" ? "closed" : "open"}">${state.breaker === "closed" ? "合" : "分"}</span>
      </div>
      <div class="toolbar-state-group">
        <span>防跳</span>
        <div class="segmented toolbar-segmented" role="group" aria-label="防跳方式">
          <button class="${state.antiPump === "device" ? "active" : ""}" data-state="antiPump" data-value="device">装置</button>
          <button class="${state.antiPump === "mechanism" ? "active" : ""}" data-state="antiPump" data-value="mechanism">机构</button>
        </div>
      </div>
      <div class="toolbar-jumper-group" role="group" aria-label="跳线选择">
        <label class="switch-label toolbar-switch"><input id="jw7" type="checkbox" ${state.jw7 ? "checked" : ""}><span></span>JW7</label>
        <label class="switch-label toolbar-switch"><input id="jw9" type="checkbox" ${state.jw9 ? "checked" : ""}><span></span>JW9</label>
      </div>`;
    return;
  }

  if (!loop.device) {
    elements.stateCard.hidden = true;
    elements.simulationControls.replaceChildren();
    return;
  }

  const device = loop.device;
  const current = getDeviceState(device);
  const copy = deviceControlCopy(device);
  elements.simulationControls.innerHTML = `
    <div class="toolbar-state-group device-control-group">
      <span>${device.code} ${copy.kind}</span>
      <div class="segmented toolbar-segmented" role="group" aria-label="${device.label}状态">
        <button class="${current ? "" : "active"}" data-device-value="off" aria-pressed="${!current}">${copy.off}</button>
        <button class="${current ? "active" : ""}" data-device-value="on" aria-pressed="${current}">${copy.on}</button>
      </div>
    </div>`;
}

function conditionText(loop) {
  if (loop.conditionText) return loop.conditionText;
  if (!loop.device) return "相关接点需满足导通条件";
  const copy = deviceControlCopy(loop.device);
  return `${loop.device.code} ${copy.kind}需${copy.on}`;
}

function renderList() {
  const visible = loops.map((loop, index) => ({ loop, index }))
    .filter(({ loop }) => activeFilter === "all" || loop.group === activeFilter);

  elements.count.textContent = visible.length;
  elements.list.innerHTML = visible.map(({ loop, index }) => `
    <button class="loop-item ${index === activeIndex ? "active" : ""}" data-index="${index}" role="option" aria-selected="${index === activeIndex}">
      <span class="number">${String(loop.row).padStart(2, "0")}</span>
      <span class="loop-name">${loop.name}</span>
      <span class="loop-tag">${loop.tag}</span>
    </button>`).join("");

  elements.list.querySelectorAll(".loop-item").forEach(button => {
    button.addEventListener("click", () => selectLoop(Number(button.dataset.index)));
  });
}

function pathEndpoint(pathData, last = false) {
  const numbers = pathData.match(/-?\d+(?:\.\d+)?/g).map(Number);
  return last ? [numbers[numbers.length - 2], numbers[numbers.length - 1]] : [numbers[0], numbers[1]];
}

function parsePolyline(pathData) {
  const points = [];
  const commandPattern = /[ML]\s*(-?\d+(?:\.\d+)?)\s+(-?\d+(?:\.\d+)?)/gi;
  let match;
  while ((match = commandPattern.exec(pathData))) points.push([Number(match[1]), Number(match[2])]);
  return points;
}

function formatPolyline(points) {
  return points.map(([x, y], index) => `${index ? "L" : "M"}${Number(x.toFixed(2))} ${Number(y.toFixed(2))}`).join(" ");
}

function pushDistinct(points, point) {
  const last = points[points.length - 1];
  if (!last || Math.abs(last[0] - point[0]) > .01 || Math.abs(last[1] - point[1]) > .01) points.push(point);
}

function splitPathTowardRelay(pathData, relayTarget) {
  const points = parsePolyline(pathData);
  if (points.length < 2) return [pathData];

  let closest = null;
  points.slice(0, -1).forEach((start, index) => {
    const end = points[index + 1];
    const dx = end[0] - start[0];
    const dy = end[1] - start[1];
    const lengthSquared = dx * dx + dy * dy;
    if (!lengthSquared) return;
    const rawT = ((relayTarget[0] - start[0]) * dx + (relayTarget[1] - start[1]) * dy) / lengthSquared;
    const t = Math.max(0, Math.min(1, rawT));
    const point = [start[0] + dx * t, start[1] + dy * t];
    const distance = Math.hypot(point[0] - relayTarget[0], point[1] - relayTarget[1]);
    if (!closest || distance < closest.distance) closest = { index, point, distance };
  });

  if (!closest || closest.distance > 24) return [pathData];

  const positiveSide = points.slice(0, closest.index + 1);
  pushDistinct(positiveSide, closest.point);
  const negativeSide = points.slice(closest.index + 1).reverse();
  pushDistinct(negativeSide, closest.point);
  return [positiveSide, negativeSide]
    .filter(segment => segment.length > 1)
    .map(formatPolyline);
}

function getDirectionalFlowPaths(loop) {
  if (loop.flow?.direction === "forward") return loop.paths;
  const target = loop.relay?.target || loop.device?.target;
  if (!target) return loop.paths;
  return loop.paths.flatMap(pathData => splitPathTowardRelay(pathData, target));
}

function splitPathAtTarget(pathData, target) {
  const points = parsePolyline(pathData);
  if (points.length < 2) return { before: pathData, after: null };

  let closest = null;
  points.slice(0, -1).forEach((start, index) => {
    const end = points[index + 1];
    const dx = end[0] - start[0];
    const dy = end[1] - start[1];
    const lengthSquared = dx * dx + dy * dy;
    if (!lengthSquared) return;
    const rawT = ((target[0] - start[0]) * dx + (target[1] - start[1]) * dy) / lengthSquared;
    const t = Math.max(0, Math.min(1, rawT));
    const point = [start[0] + dx * t, start[1] + dy * t];
    const distance = Math.hypot(point[0] - target[0], point[1] - target[1]);
    if (!closest || distance < closest.distance) closest = { index, point, distance };
  });

  if (!closest || closest.distance > 24) return { before: pathData, after: null };
  const beforePoints = points.slice(0, closest.index + 1);
  pushDistinct(beforePoints, closest.point);
  const afterPoints = [closest.point, ...points.slice(closest.index + 1)];
  return {
    before: beforePoints.length > 1 ? formatPolyline(beforePoints) : null,
    after: afterPoints.length > 1 ? formatPolyline(afterPoints) : null
  };
}

function isDeviceStop(loop) {
  return Boolean(loop.device && isOperationReady(loop) && !getDeviceState(loop.device) && loop.flow?.gateIndexes?.length);
}

function getFlowPresentation(loop, ready) {
  if (ready) return { livePaths: getDirectionalFlowPaths(loop), blockedPaths: [], stopped: false };
  if (!isDeviceStop(loop)) return { livePaths: [], blockedPaths: getDirectionalFlowPaths(loop), stopped: false };

  const gated = new Set(loop.flow.gateIndexes);
  const livePaths = [];
  const blockedPaths = [];
  loop.paths.forEach((pathData, index) => {
    if (!gated.has(index)) {
      livePaths.push(pathData);
      return;
    }
    const parts = splitPathAtTarget(pathData, loop.device.target);
    if (parts.before) livePaths.push(parts.before);
    if (parts.after) blockedPaths.push(parts.after);
  });
  return { livePaths, blockedPaths, stopped: true };
}

function createSvgElement(name, attributes = {}) {
  const node = document.createElementNS("http://www.w3.org/2000/svg", name);
  Object.entries(attributes).forEach(([key, value]) => node.setAttribute(key, value));
  return node;
}

function renderLamp(loop, ready) {
  elements.lampOverlay.replaceChildren();
  if (!loop.lamp) return;

  const x = loop.lamp.target[0];
  const y = 297 - loop.lamp.target[1];
  const group = createSvgElement("g", { class: `circuit-indicator ${ready ? "lit" : "off"}` });
  const halo = createSvgElement("circle", { cx: x, cy: y, r: 7.2, class: "indicator-halo" });
  const ring = createSvgElement("circle", { cx: x, cy: y, r: 4.2, class: "indicator-ring" });
  const core = createSvgElement("circle", { cx: x, cy: y, r: 2.3, class: "indicator-core" });
  const label = createSvgElement("text", { x, y: y - 8.5, class: "indicator-label", "text-anchor": "middle" });
  label.textContent = ready ? `${loop.lamp.code}亮` : `${loop.lamp.code}灭`;
  group.append(halo, ring, core, label);
  elements.lampOverlay.append(group);
}

function renderDevice(loop) {
  elements.deviceOverlay.replaceChildren();
  if (!loop.device) return;

  const device = loop.device;
  const current = getDeviceState(device);
  const copy = deviceControlCopy(device);
  const x = device.target[0];
  const y = 297 - device.target[1];
  const group = createSvgElement("g", {
    class: `interactive-device device-${device.type} ${current ? "on" : "off"}`,
    role: "button",
    "aria-label": `${device.label}，当前${current ? copy.on : copy.off}，点击切换`
  });
  const halo = createSvgElement("circle", { cx: x, cy: y, r: 9, class: "device-halo" });
  const label = createSvgElement("text", { x, y: y - 10.5, class: "device-state-label", "text-anchor": "middle" });
  label.textContent = `${device.code} ${current ? copy.on : copy.off}`;
  group.append(halo);

  if (device.type === "button") {
    const frame = createSvgElement("circle", { cx: x, cy: y, r: 5.2, class: "device-frame" });
    const face = createSvgElement("circle", { cx: x, cy: y + (current ? 1.2 : -1.2), r: current ? 3.1 : 3.8, class: "device-button-face" });
    group.append(frame, face);
  } else {
    const frame = createSvgElement("rect", { x: x - 8, y: y - 6, width: 16, height: 12, rx: 2.2, class: "device-frame" });
    const left = createSvgElement("circle", { cx: x - 5, cy: y + 1.5, r: 1.25, class: "device-terminal" });
    const right = createSvgElement("circle", { cx: x + 5, cy: y + 1.5, r: 1.25, class: "device-terminal" });
    const blade = createSvgElement("line", {
      x1: x - 5, y1: y + 1.5, x2: x + 5, y2: current ? y + 1.5 : y - 3.8, class: "device-blade"
    });
    group.append(frame, left, right, blade);
  }

  group.append(label);
  group.addEventListener("pointerdown", event => event.stopPropagation());
  group.addEventListener("click", event => {
    event.stopPropagation();
    setDeviceState(device, !getDeviceState(device));
  });
  elements.deviceOverlay.append(group);
}

function positionRelayGuide() {
  if (!currentRelay || !elements.viewport.classList.contains("has-relay-panel")) return;

  const viewportRect = elements.viewport.getBoundingClientRect();
  const diagramRect = elements.svg.getBoundingClientRect();
  const panelRect = elements.relayPanel.getBoundingClientRect();
  const scale = Math.min(diagramRect.width / currentViewBox.w, diagramRect.height / currentViewBox.h);
  const contentWidth = currentViewBox.w * scale;
  const contentHeight = currentViewBox.h * scale;
  const contentLeft = diagramRect.left - viewportRect.left + (diagramRect.width - contentWidth) / 2;
  const contentTop = diagramRect.top - viewportRect.top + (diagramRect.height - contentHeight) / 2;
  const targetX = contentLeft + (currentRelay.target[0] - currentViewBox.x) * scale;
  const targetViewY = 297 - currentRelay.target[1];
  const targetY = contentTop + (targetViewY - currentViewBox.y) * scale;
  const contentBottom = contentTop + contentHeight;
  const endY = Math.max(contentTop + 8, Math.min(contentBottom - 8, targetY));
  const startX = panelRect.right - viewportRect.left + 3;
  const startY = panelRect.top - viewportRect.top + panelRect.height / 2;
  const endX = contentLeft - 10;
  const elbowX = startX + Math.max(4, (endX - startX) * .55);
  const tailStartX = contentLeft + 2;
  const tailEndX = Math.max(tailStartX + 4, Math.min(contentLeft + contentWidth - 8, targetX - 11));

  elements.guide.setAttribute("viewBox", `0 0 ${viewportRect.width} ${viewportRect.height}`);
  elements.guidePath.setAttribute("d", `M${startX} ${startY} H${elbowX} V${endY} H${endX}`);
  elements.guideTail.setAttribute("d", `M${tailStartX} ${endY} H${tailEndX}`);
  elements.guideLabel.setAttribute("x", endX - 5);
  elements.guideLabel.setAttribute("y", endY - 7);
  elements.guideLabel.textContent = currentRelay.code;
}

function renderRelay(loop, ready) {
  if (relayTimer) window.clearTimeout(relayTimer);
  relayTimer = null;
  elements.relay.replaceChildren();
  elements.relayPanel.className = "relay-side-panel";
  elements.relayPanel.setAttribute("aria-hidden", "true");
  elements.guide.className.baseVal = "relay-guide";
  elements.guidePath.removeAttribute("d");
  elements.guideTail.removeAttribute("d");
  elements.guideLabel.textContent = "";
  currentRelay = null;
  elements.viewport.classList.remove("has-relay-panel");
  if (!loop.relay) return;
  elements.viewport.classList.add("has-relay-panel");

  const relay = loop.relay;
  currentRelay = relay;
  const targetX = relay.target[0];
  const targetY = 297 - relay.target[1];

  const group = createSvgElement("g", { class: `relay-demo ${ready ? "waiting" : "blocked"}` });
  const drawRelaySymbol = !loop.device || Math.hypot(
    relay.target[0] - loop.device.target[0], relay.target[1] - loop.device.target[1]
  ) > 14;
  if (drawRelaySymbol) {
    const pulse = createSvgElement("circle", { cx: targetX, cy: targetY, r: 8, class: "relay-pulse" });
    const focus = createSvgElement("circle", { cx: targetX, cy: targetY, r: 5.2, class: "relay-focus" });
    const coil = createSvgElement("rect", {
      x: targetX - 4.5, y: targetY - 3.2, width: 9, height: 6.4, rx: 1.2, class: "relay-coil"
    });
    const code = createSvgElement("text", {
      x: targetX, y: targetY - 6.5, class: "relay-code-label", "text-anchor": "middle"
    });
    code.textContent = relay.code;

    const contactLeft = createSvgElement("line", {
      x1: targetX + 6, y1: targetY, x2: targetX + 9, y2: targetY, class: "relay-contact-fixed"
    });
    const contactRight = createSvgElement("line", {
      x1: targetX + 18, y1: targetY, x2: targetX + 21, y2: targetY, class: "relay-contact-fixed"
    });
    const blade = createSvgElement("line", {
      x1: targetX + 9, y1: targetY, x2: targetX + 18, y2: targetY - 4,
      class: "relay-contact-blade",
      style: `transform-origin:${targetX + 9}px ${targetY}px`
    });
    group.append(pulse, focus, coil, code, contactLeft, contactRight, blade);
  }
  elements.relay.append(group);

  elements.relayPanel.classList.add("left");
  elements.relayDirection.textContent = "图中动作位置 →";
  elements.relayCode.textContent = relay.code;
  elements.relayName.textContent = relay.name;
  elements.relayAction.textContent = ready ? relay.action : "当前不动作";
  elements.relayPrinciple.textContent = ready
    ? relay.principle
    : `条件未满足，${relay.code} 保持原状态；请先调整顶部仿真条件。`;
  window.requestAnimationFrame(positionRelayGuide);

  if (ready) {
    relayTimer = window.setTimeout(() => {
      if (!elements.relay.contains(group)) return;
      group.classList.remove("waiting");
      group.classList.add("arrived");
      elements.relayPanel.classList.add("visible");
      elements.guide.classList.add("visible");
      elements.relayPanel.setAttribute("aria-hidden", "false");
      window.requestAnimationFrame(positionRelayGuide);
      if (loops[activeIndex] === loop && isReady(loop)) elements.status.textContent = `${relay.code} 动态动作`;
    }, 1250 / Number(elements.speed.value));
  } else {
    elements.relayPanel.classList.add("visible", "blocked");
    elements.guide.classList.add("visible", "blocked");
    elements.relayPanel.setAttribute("aria-hidden", "false");
    window.requestAnimationFrame(positionRelayGuide);
  }
}

function renderOverlay(loop, ready) {
  elements.overlay.replaceChildren();
  const presentation = getFlowPresentation(loop, ready);
  const drawPaths = (paths, blocked, pathOffset = 0) => paths.forEach((pathData, pathIndex) => {
    const stateClass = blocked ? " blocked" : "";
    const halo = createSvgElement("path", { d: pathData, class: `trace-halo${stateClass}` });
    const flow = createSvgElement("path", { d: pathData, class: `trace-flow${stateClass}` });
    elements.overlay.append(halo, flow);

    const start = pathEndpoint(pathData);
    const end = pathEndpoint(pathData, true);
    [start, end].forEach(([cx, cy]) => elements.overlay.append(createSvgElement("circle", {
      cx, cy, r: 1.45, class: `flow-terminal${stateClass}`
    })));

    if (!blocked) {
      const dot = createSvgElement("circle", { r: 1.65, class: "current-dot" });
      const motion = createSvgElement("animateMotion", {
        dur: `${2.5 + (pathIndex + pathOffset) * .35}s`, repeatCount: "indefinite", path: pathData
      });
      dot.append(motion);
      elements.overlay.append(dot);
    }
  });
  drawPaths(presentation.livePaths, false);
  drawPaths(presentation.blockedPaths, true, presentation.livePaths.length);
  renderLamp(loop, ready);
  renderDevice(loop);
  const relayReady = loop.flow?.relayIndependent ? isOperationReady(loop) : ready;
  renderRelay(loop, relayReady);
}

function selectLoop(index, { keepAutoplay = false } = {}) {
  activeIndex = (index + loops.length) % loops.length;
  const loop = loops[activeIndex];
  const ready = isReady(loop);
  const stopped = isDeviceStop(loop);
  renderSimulationControls(loop);

  elements.index.textContent = String(loop.row).padStart(2, "0");
  elements.title.textContent = loop.name;
  elements.summary.textContent = ready
    ? loop.summary
    : stopped
      ? `${loop.summary} 当前 ${loop.device.code} 已${deviceControlCopy(loop.device).off}，电位到达该图元后停止，后段不再流动。`
      : `${loop.summary} 当前教学条件未满足：${conditionText(loop)}。`;
  elements.status.textContent = ready ? "路径已导通" : stopped ? `流至 ${loop.device.code} 停止` : "条件未满足";
  elements.status.className = `status-pill ${ready ? "ready" : "blocked"}`;
  elements.chips.innerHTML = loop.components.map(item => `<span class="component-chip">${item}</span>`).join("");
  renderOverlay(loop, ready);
  renderList();

  const activeButton = elements.list.querySelector(".loop-item.active");
  activeButton?.scrollIntoView({ block: "nearest", behavior: "smooth" });
  if (!keepAutoplay && autoplayTimer) stopAutoplay();
}

function updateState(name, value) {
  state[name] = value;
  selectLoop(activeIndex, { keepAutoplay: true });
}

function setViewBox(next) {
  const minW = 62;
  const maxW = 420;
  next.w = Math.max(minW, Math.min(maxW, next.w));
  next.h = next.w * 297 / 420;
  next.x = Math.max(0, Math.min(420 - next.w, next.x));
  next.y = Math.max(0, Math.min(297 - next.h, next.y));
  currentViewBox = next;
  elements.svg.setAttribute("viewBox", `${next.x} ${next.y} ${next.w} ${next.h}`);
  window.requestAnimationFrame(positionRelayGuide);
}

function zoom(factor, clientX, clientY) {
  const rect = elements.svg.getBoundingClientRect();
  const rx = clientX == null ? .5 : (clientX - rect.left) / rect.width;
  const ry = clientY == null ? .5 : (clientY - rect.top) / rect.height;
  const old = currentViewBox;
  const w = old.w * factor;
  const h = w * 297 / 420;
  setViewBox({ x: old.x + (old.w - w) * rx, y: old.y + (old.h - h) * ry, w, h });
}

function startAutoplay() {
  stopAutoplay();
  elements.play.classList.add("playing");
  elements.play.querySelector(".play-icon").textContent = "Ⅱ";
  elements.play.querySelector(".play-label").textContent = "暂停讲解";
  const speed = Number(elements.speed.value);
  autoplayTimer = window.setInterval(() => selectLoop(activeIndex + 1, { keepAutoplay: true }), 4400 / speed);
}

function stopAutoplay() {
  if (autoplayTimer) window.clearInterval(autoplayTimer);
  autoplayTimer = null;
  elements.play.classList.remove("playing");
  elements.play.querySelector(".play-icon").textContent = "▶";
  elements.play.querySelector(".play-label").textContent = "自动讲解";
}

elements.stateCard.addEventListener("click", event => {
  const button = event.target.closest("button");
  if (!button || !elements.stateCard.contains(button)) return;
  if (button.dataset.state) updateState(button.dataset.state, button.dataset.value);
  if (button.dataset.deviceValue) {
    const device = loops[activeIndex].device;
    if (device) setDeviceState(device, button.dataset.deviceValue === "on");
  }
});

elements.stateCard.addEventListener("change", event => {
  if (event.target.id === "jw7" || event.target.id === "jw9") updateState(event.target.id, event.target.checked);
});

document.querySelectorAll(".view-mode").forEach(button => button.addEventListener("click", () => {
  document.querySelectorAll(".view-mode").forEach(item => item.classList.toggle("active", item === button));
  elements.viewport.classList.remove("mode-original", "mode-focus");
  if (button.dataset.mode !== "teaching") elements.viewport.classList.add(`mode-${button.dataset.mode}`);
}));

document.getElementById("zoomIn").addEventListener("click", () => zoom(.78));
document.getElementById("zoomOut").addEventListener("click", () => zoom(1.28));
document.getElementById("fitView").addEventListener("click", () => setViewBox({ x: 0, y: 0, w: 420, h: 297 }));
document.getElementById("fullscreen").addEventListener("click", () => {
  if (!document.fullscreenElement) elements.viewport.requestFullscreen?.();
  else document.exitFullscreen?.();
});

elements.svg.addEventListener("wheel", event => {
  event.preventDefault();
  zoom(event.deltaY > 0 ? 1.13 : .885, event.clientX, event.clientY);
}, { passive: false });

elements.svg.addEventListener("pointerdown", event => {
  dragging = { x: event.clientX, y: event.clientY, viewBox: { ...currentViewBox } };
  elements.svg.setPointerCapture(event.pointerId);
  elements.svg.classList.add("dragging");
});

elements.svg.addEventListener("pointermove", event => {
  if (!dragging) return;
  const rect = elements.svg.getBoundingClientRect();
  const dx = (event.clientX - dragging.x) * dragging.viewBox.w / rect.width;
  const dy = (event.clientY - dragging.y) * dragging.viewBox.h / rect.height;
  setViewBox({ ...dragging.viewBox, x: dragging.viewBox.x - dx, y: dragging.viewBox.y - dy });
});

elements.svg.addEventListener("pointerup", event => {
  dragging = null;
  elements.svg.releasePointerCapture(event.pointerId);
  elements.svg.classList.remove("dragging");
});

elements.play.addEventListener("click", () => autoplayTimer ? stopAutoplay() : startAutoplay());
elements.next.addEventListener("click", () => selectLoop(activeIndex + 1));
elements.speed.addEventListener("change", () => {
  document.documentElement.style.setProperty("--flow-speed", `${1.8 / Number(elements.speed.value)}s`);
  if (autoplayTimer) startAutoplay();
});
window.addEventListener("resize", () => window.requestAnimationFrame(positionRelayGuide));

document.addEventListener("keydown", event => {
  if (event.key === "ArrowRight") selectLoop(activeIndex + 1);
  if (event.key === "ArrowLeft") selectLoop(activeIndex - 1);
  if (event.code === "Space" && event.target === document.body) {
    event.preventDefault();
    autoplayTimer ? stopAutoplay() : startAutoplay();
  }
});

elements.drawingSelect.innerHTML = drawingPages.map(page => `<option value="${page.id}">${page.id} · ${page.shortTitle}</option>`).join("");
elements.drawingSelect.addEventListener("change", event => selectPage(event.target.value));
selectPage("31-15");
