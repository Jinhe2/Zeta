const pageLoop = (row, name, tag, group, summary, components, paths, relay, device) => ({
  row, name, tag, group, summary, components, paths,
  ...(relay ? { relay } : {}),
  ...(device ? { device } : {})
});

const outputRelay = (code, name, x, y, principle, action = "触点闭合") => ({
  code, name, target: [x, y], action, principle
});

const interactiveDevice = (id, code, label, type, x, y, defaultState = true) => ({
  id, code, label, type, target: [x, y], defaultState
});

const additionalDrawingPages = [
  {
    id: "31-07", title: "交直流电源回路图", shortTitle: "交直流电源", entityCount: 718,
    note: "公共图纸，仅展示 1n 装置及柜内公共电源部分。",
    filters: [{ id: "dc", label: "直流" }, { id: "ac", label: "交流" }, { id: "ground", label: "接地" }],
    loops: [
      pageLoop(1, "1n 装置电源", "1-1DK", "dc", "直流电源经 1-1DK 双极开关送入 1n 保护装置，为装置 CPU、保护及通信模块供电。", ["+KM", "-KM", "1-1DK", "1n 装置"], ["M44 264 L72 264 L84 264 L148 264", "M44 256 L72 256 L84 256 L148 256"], outputRelay("1-1DK", "装置电源开关", 78, 260, "双极开关同时接通正、负直流电源；分开后装置整体失电。", "双极合闸"), interactiveDevice("dk11", "1-1DK", "装置电源空开", "breaker", 78, 260)),
      pageLoop(2, "1n 开入电源", "1-2DK", "dc", "开入电源经独立空开送入 1n 装置开入回路，使外部接点状态可被装置采集。", ["开入+", "开入-", "1-2DK", "1n 开入板"], ["M44 228 L72 228 L84 228 L148 228", "M44 220 L72 220 L84 220 L148 220"], outputRelay("1-2DK", "开入电源开关", 78, 224, "独立开入电源用于接点量采集，与装置主电源分开便于检修和故障隔离。", "双极合闸"), interactiveDevice("dk12", "1-2DK", "开入电源空开", "breaker", 78, 224)),
      pageLoop(3, "电压切换电源", "1-3DK", "dc", "切换电源经 1-3DK 送至电压切换箱，供 PT 切换继电器及位置监视使用。", ["切换+", "切换-", "1-3DK", "电压切换箱"], ["M44 208 L72 208 L84 208 L148 208", "M44 200 L72 200 L84 200 L148 200"], outputRelay("1-3DK", "切换电源开关", 78, 204, "该双极开关给电压切换继电器提供独立直流电源，失电时电压回路不能完成自动切换。", "双极合闸"), interactiveDevice("dk13", "1-3DK", "切换电源空开", "breaker", 78, 204)),
      pageLoop(4, "1n 操作电源", "1-4DK", "dc", "操作电源经 1-4DK 送入 1n 操作回路，为合闸、跳闸、保持及监视支路供电。", ["操作+", "操作-", "1-4DK", "1n 操作箱"], ["M44 188 L72 188 L84 188 L148 188", "M44 180 L72 180 L84 180 L148 180"], outputRelay("1-4DK", "操作电源开关", 78, 184, "双极操作电源开关接通后，跳合闸线圈及其监视继电器才具备工作电源。", "双极合闸"), interactiveDevice("dk14", "1-4DK", "操作电源空开", "breaker", 78, 184)),
      pageLoop(5, "柜内照明电源", "1ZK", "ac", "交流 L、N 经照明开关送至柜内照明灯，为巡视和检修提供照明。", ["L", "N", "1ZK", "柜内照明灯"], ["M260 264 L300 264 L312 264 L344 264", "M260 252 L344 252"], outputRelay("1ZK", "照明电源开关", 304, 264, "照明开关闭合后，交流相线与中性线形成供电回路，柜内照明灯点亮。", "开关闭合"), interactiveDevice("zk1", "1ZK", "柜内照明空开", "breaker", 304, 264)),
      pageLoop(6, "打印机电源", "2ZK", "ac", "交流电源经打印机电源开关送至柜内打印机插座。", ["L", "N", "2ZK", "打印机"], ["M260 232 L300 232 L312 232 L344 232", "M260 222 L344 222"], outputRelay("2ZK", "打印机电源开关", 304, 232, "开关闭合后为打印机提供交流电源；检修打印机时可单独隔离。", "开关闭合"), interactiveDevice("zk2", "2ZK", "打印机电源空开", "breaker", 304, 232)),
      pageLoop(7, "柜体与装置接地", "PE", "ground", "柜体、保护装置和电缆屏蔽层可靠连接保护地，泄放故障电流并抑制干扰。", ["保护地排", "装置接地", "柜体接地", "电缆屏蔽"], ["M188 210 L220 210 L220 188", "M220 210 L252 210"])
    ]
  },
  {
    id: "31-08", title: "通信校时回路图", shortTitle: "通信与校时", entityCount: 535,
    note: "公共图纸，仅展示 1n 通信、校时及打印通道。",
    filters: [{ id: "network", label: "网络" }, { id: "time", label: "校时" }, { id: "print", label: "打印" }],
    loops: [
      pageLoop(1, "以太网通道 1", "ETH1", "network", "1n 装置 ETH1 光/电以太网口接至站控层网络，为保护信息和定值管理提供主通信通道。", ["ETH1", "1n 装置", "站控层网络"], ["M164 258 L252 258 L344 258"]),
      pageLoop(2, "以太网通道 2", "ETH2", "network", "ETH2 作为独立网络接口，可用于双网冗余或另一业务网通信。", ["ETH2", "1n 装置", "冗余网络"], ["M164 242 L252 242 L344 242"]),
      pageLoop(3, "以太网通道 3", "ETH3", "network", "ETH3 提供第三路独立网络连接，用于工程配置或专用数据通道。", ["ETH3", "1n 装置", "工程/专网"], ["M164 226 L252 226 L344 226"]),
      pageLoop(4, "IRIG-B 对时", "IRIG-B", "time", "IRIG-B+、IRIG-B- 差分信号送入 1n 装置，为故障记录和保护事件提供统一时标。", ["IRIG-B+", "IRIG-B-", "对时装置", "1n 装置"], ["M164 206 L252 206 L344 206", "M164 198 L252 198 L344 198"]),
      pageLoop(5, "RS-485 通信", "RS485", "network", "RS485+、RS485- 构成差分串行通道，适用于低速可靠的设备数据通信。", ["RS485+", "RS485-", "屏蔽双绞线"], ["M164 174 L252 174 L344 174", "M164 166 L252 166 L344 166"]),
      pageLoop(6, "打印数据通道", "TXD/GND", "print", "装置 TXD 与 GND 接入打印机串口，用于保护动作报告和事件记录打印。", ["TXD", "GND", "打印机串口"], ["M164 144 L252 144 L344 144", "M164 132 L252 132 L344 132"]),
      pageLoop(7, "打印切换把手", "PK", "print", "打印切换把手选择 1n 装置打印通道，避免两套装置同时驱动打印机。", ["PK", "1n 打印", "打印机"], ["M164 121 L188 121 L202 121 L252 121", "M164 105 L188 105 L202 105 L252 105"], outputRelay("PK", "打印切换把手", 190, 121, "把手切到 1n 位置时，对应串行打印通道接通；另一装置通道保持隔离。", "切至 1n"))
    ]
  },
  {
    id: "31-09", title: "1-1n 电流电压回路图", shortTitle: "电流电压", entityCount: 717,
    note: "1-1n 装置模拟量采样回路。",
    filters: [{ id: "voltage", label: "电压" }, { id: "current", label: "电流" }],
    loops: [
      pageLoop(1, "A 相电压采样", "Ua", "voltage", "母线 A 相电压经端子和电压切换回路送入 1n 装置电压采样端。", ["Ua", "PT 二次", "1n 电压输入"], ["M250 266 L286 266 L318 266"]),
      pageLoop(2, "B 相电压采样", "Ub", "voltage", "母线 B 相电压送入 1n 装置，用于相间故障和功率方向判别。", ["Ub", "PT 二次", "1n 电压输入"], ["M250 258 L286 258 L318 258"]),
      pageLoop(3, "C 相电压采样", "Uc", "voltage", "母线 C 相电压送入 1n 装置，与 Ua、Ub 共同形成三相电压量。", ["Uc", "PT 二次", "1n 电压输入"], ["M250 250 L286 250 L318 250"]),
      pageLoop(4, "线路抽取电压", "Ux", "voltage", "线路侧抽取电压 Ux 送入装置，供重合闸同期或无压判别使用。", ["Ux", "线路 PT", "同期/无压判别"], ["M250 234 L286 234 L318 234"]),
      pageLoop(5, "A 相保护电流", "Ia", "current", "CT A 相二次电流串联送入 1n 保护电流输入，回路不得开路。", ["Ia", "CT 二次", "1n 电流输入"], ["M250 182 L292 182 L334 182"]),
      pageLoop(6, "B 相保护电流", "Ib", "current", "CT B 相二次电流送入装置，参与差动、距离及过流保护计算。", ["Ib", "CT 二次", "1n 电流输入"], ["M250 174 L292 174 L334 174"]),
      pageLoop(7, "C 相保护电流", "Ic", "current", "CT C 相二次电流送入装置，参与三相故障量计算。", ["Ic", "CT 二次", "1n 电流输入"], ["M250 166 L292 166 L334 166"]),
      pageLoop(8, "零序电流", "I0", "current", "零序 CT 二次电流送入装置，用于接地故障和零序过流保护判别。", ["I0", "零序 CT", "接地保护"], ["M250 158 L292 158 L334 158"])
    ]
  },
  {
    id: "31-11", title: "1-1n 开入回路图", shortTitle: "开入回路", entityCount: 730,
    note: "1-1n 装置硬接点开入及状态采集回路。",
    filters: [{ id: "control", label: "控制" }, { id: "protect", label: "保护" }, { id: "breaker", label: "开关" }],
    loops: [
      pageLoop(1, "远方操作允许", "KLP5", "control", "远方/就地压板接点送入装置，决定远方控制命令是否允许执行。", ["KLP5", "远方操作", "开入公共负"], ["M45 264 L92 264 L150 264"], null, interactiveDevice("klp5", "KLP5", "远方操作压板", "plate", 92, 264)),
      pageLoop(2, "装置检修状态", "检修", "control", "检修压板投入后，装置识别检修状态并对远传信息作相应处理。", ["检修压板", "装置检修开入"], ["M45 256 L92 256 L150 256"], null, interactiveDevice("repair", "检修", "检修压板", "plate", 92, 256)),
      pageLoop(3, "信号复归", "复归", "control", "外部复归按钮接点闭合后，装置复归可复归的告警和动作信号。", ["复归按钮", "信号复归开入"], ["M45 242 L92 242 L150 242"], null, interactiveDevice("reset", "复归", "信号复归按钮", "button", 92, 242, false)),
      pageLoop(4, "投差动保护", "KLP1", "protect", "差动保护压板状态送入装置，作为差动保护投入条件。", ["KLP1", "投差动", "开入端子"], ["M233 264 L286 264 L340 264"], null, interactiveDevice("klp1", "KLP1", "差动保护压板", "plate", 286, 264)),
      pageLoop(5, "投距离保护", "KLP2", "protect", "距离保护压板状态送入装置，决定距离保护逻辑是否投入。", ["KLP2", "投距离", "开入端子"], ["M233 256 L286 256 L340 256"], null, interactiveDevice("klp2", "KLP2", "距离保护压板", "plate", 286, 256)),
      pageLoop(6, "投零序过流", "KLP3", "protect", "零序过流压板接点送入装置，作为零序过流保护投入条件。", ["KLP3", "零序过流", "开入端子"], ["M233 248 L286 248 L340 248"], null, interactiveDevice("klp3", "KLP3", "零序过流压板", "plate", 286, 248)),
      pageLoop(7, "停用重合闸", "KLP4", "protect", "停用重合闸压板投入时，装置闭锁自动重合闸功能。", ["KLP4", "停用重合闸", "开入端子"], ["M233 240 L286 240 L340 240"], null, interactiveDevice("klp4", "KLP4", "停用重合闸压板", "plate", 286, 240)),
      pageLoop(8, "外部闭锁重合闸", "BSCH", "protect", "外部闭锁接点动作后，装置禁止重合闸启动或出口。", ["BSCH", "闭锁重合闸", "开入端子"], ["M233 216 L286 216 L340 216"]),
      pageLoop(9, "远传开入 1", "YC1", "control", "备用远传接点 1 送入装置，可组态为站内辅助状态量。", ["YC1", "远传 1", "开入端子"], ["M233 200 L286 200 L340 200"]),
      pageLoop(10, "远传开入 2", "YC2", "control", "备用远传接点 2 送入装置，可组态为另一辅助状态量。", ["YC2", "远传 2", "开入端子"], ["M233 192 L286 192 L340 192"]),
      pageLoop(11, "其他保护动作", "QTBH", "protect", "其他保护装置动作接点送入 1n 装置，用于联动闭锁或记录。", ["其他保护动作", "联动开入"], ["M233 184 L286 184 L340 184"]),
      pageLoop(12, "机构低气压/弹簧未储能", "JGDY", "breaker", "断路器机构异常接点送入装置，参与合闸闭锁和告警。", ["低气压", "弹簧未储能", "机构开入"], ["M233 176 L286 176 L340 176"]),
      pageLoop(13, "断路器跳位", "TW", "breaker", "断路器跳位辅助接点送入装置，提供一次设备分位状态。", ["跳位接点", "断路器位置"], ["M233 168 L286 168 L340 168"]),
      pageLoop(14, "断路器合位", "HW", "breaker", "断路器合位辅助接点送入装置，提供一次设备合位状态。", ["合位接点", "断路器位置"], ["M233 160 L286 160 L340 160"])
    ]
  },
  {
    id: "31-13", title: "1-1n 电压切换回路图", shortTitle: "电压切换", entityCount: 717,
    note: "1-1n 母线 PT 自动切换及电压监视回路。",
    filters: [{ id: "switch", label: "切换" }, { id: "measure", label: "计量" }, { id: "monitor", label: "监视" }],
    loops: [
      pageLoop(1, "Ⅰ母 PT 切换", "1ZJ", "switch", "Ⅰ母隔离开关位置满足时，Ⅰ母切换继电器动作，将Ⅰ母 PT 二次电压接入保护装置。", ["Ⅰ母刀闸位置", "1ZJ", "Ⅰ母 PT", "1n 装置"], ["M76 258 L164 258 L222 258 L318 258", "M76 250 L164 250 L222 250 L318 250"], outputRelay("1ZJ", "Ⅰ母电压切换继电器", 230, 258, "Ⅰ母位置接点使切换继电器得电，其多组触点同步接通Ⅰ母三相电压。", "得电切换")),
      pageLoop(2, "Ⅱ母 PT 切换", "2ZJ", "switch", "Ⅱ母隔离开关位置满足时，Ⅱ母切换继电器动作，将Ⅱ母 PT 二次电压接入保护装置。", ["Ⅱ母刀闸位置", "2ZJ", "Ⅱ母 PT", "1n 装置"], ["M76 226 L164 226 L222 226 L318 226", "M76 218 L164 218 L222 218 L318 218"], outputRelay("2ZJ", "Ⅱ母电压切换继电器", 230, 226, "Ⅱ母位置接点使切换继电器得电，其多组触点同步接通Ⅱ母三相电压。", "得电切换")),
      pageLoop(3, "Ⅰ母 PT 计量电压", "Ⅰ母PT", "measure", "Ⅰ母 PT 的 A、B、C 三相二次电压经端子送往计量和监视回路。", ["Ⅰ母 Ua", "Ⅰ母 Ub", "Ⅰ母 Uc", "计量端子"], ["M76 178 L164 178 L318 178", "M76 170 L164 170 L318 170", "M76 162 L164 162 L318 162"]),
      pageLoop(4, "Ⅱ母 PT 计量电压", "Ⅱ母PT", "measure", "Ⅱ母 PT 的 A、B、C 三相二次电压经端子送往计量和监视回路。", ["Ⅱ母 Ua", "Ⅱ母 Ub", "Ⅱ母 Uc", "计量端子"], ["M76 150 L164 150 L318 150", "M76 142 L164 142 L318 142", "M76 134 L164 134 L318 134"]),
      pageLoop(5, "Ⅰ母 PT 位置监视", "1PT", "monitor", "Ⅰ母 PT 投入位置接点用于监视当前电压来源和切换状态。", ["1PT", "位置接点", "切换监视"], ["M176 267 L206 267 L244 267"], outputRelay("1PT", "Ⅰ母 PT 位置继电器", 207, 267, "位置继电器反映Ⅰ母 PT 接入状态，为切换逻辑提供互锁和显示信号。", "状态确认")),
      pageLoop(6, "Ⅱ母 PT 位置监视", "2PT", "monitor", "Ⅱ母 PT 投入位置接点用于监视当前电压来源和切换状态。", ["2PT", "位置接点", "切换监视"], ["M176 235 L206 235 L244 235"], outputRelay("2PT", "Ⅱ母 PT 位置继电器", 207, 235, "位置继电器反映Ⅱ母 PT 接入状态，并与Ⅰ母通道形成互锁。", "状态确认"))
    ]
  },
  {
    id: "31-17", title: "保护开出回路图", shortTitle: "保护开出", entityCount: 545,
    note: "公共图纸，仅展示上半部 1n 装置开出接点。",
    filters: [{ id: "trip", label: "跳闸" }, { id: "close", label: "重合" }, { id: "remote", label: "远传" }],
    loops: [
      pageLoop(1, "跳闸开出 1", "TZ-1", "trip", "1n 装置主跳闸出口接点闭合，将保护跳闸命令送往操作回路。", ["TZ-1", "保护出口", "操作回路"], ["M167 265 L232 265 L250 265 L335 265"], outputRelay("TZ-1", "主跳闸出口继电器", 241, 265, "保护判据满足后出口继电器动作，常开接点闭合并把直流跳闸命令送出。")),
      pageLoop(2, "重合闸开出 1", "HZ-1", "close", "重合闸条件满足后，1n 装置重合闸出口接点闭合，启动合闸回路。", ["HZ-1", "重合闸出口", "合闸回路"], ["M167 257 L232 257 L250 257 L335 257"], outputRelay("HZ-1", "重合闸出口继电器", 241, 257, "重合闸逻辑允许时继电器动作，触点闭合并向操作箱发出合闸脉冲。")),
      pageLoop(3, "跳闸备用 2", "TZ-2", "trip", "第二组独立跳闸接点可用于另一跳闸线圈、母差启动或备用出口。", ["TZ-2", "备用跳闸", "独立接点"], ["M167 249 L232 249 L250 249 L335 249"], outputRelay("TZ-2", "第二跳闸出口继电器", 241, 249, "独立出口接点与主跳闸接点电气隔离，可按工程需要接入另一跳闸链路。")),
      pageLoop(4, "闭锁重合闸开出", "BC", "trip", "保护动作需要禁止重合闸时，BC 出口接点闭合并送出闭锁命令。", ["BC", "闭锁重合闸", "外部重合闸"], ["M167 241 L232 241 L250 241 L335 241"], outputRelay("BC", "闭锁重合闸继电器", 241, 241, "永久性故障或指定保护动作时，继电器触点闭合，使重合闸逻辑退出。")),
      pageLoop(5, "跳闸备用 3", "TZ-3", "trip", "第三组跳闸出口作为扩展或备用硬接点使用。", ["TZ-3", "备用跳闸", "独立接点"], ["M167 233 L232 233 L250 233 L335 233"], outputRelay("TZ-3", "第三跳闸出口继电器", 241, 233, "该独立常开接点随对应保护出口动作，便于扩展外部联动。")),
      pageLoop(6, "重合闸备用 2", "HZ-2", "close", "第二组重合闸出口为备用或并行合闸命令提供独立接点。", ["HZ-2", "备用重合", "独立接点"], ["M167 225 L232 225 L250 225 L335 225"], outputRelay("HZ-2", "第二重合闸出口继电器", 241, 225, "主重合闸逻辑动作时，该独立接点可同步输出另一组合闸命令。")),
      pageLoop(7, "远传开出 1", "YC1-1", "remote", "可编程远传出口 1 将装置内部状态转换为无源接点送往外部系统。", ["YC1-1", "可编程开出", "远方系统"], ["M167 217 L232 217 L250 217 L335 217"], outputRelay("YC1-1", "远传出口继电器 1", 241, 217, "装置内相应逻辑置位后，出口继电器动作并向监控或辅助设备提供无源接点。")),
      pageLoop(8, "远传开出 2", "YC2-1", "remote", "可编程远传出口 2 提供第二路独立外部状态接点。", ["YC2-1", "可编程开出", "远方系统"], ["M167 209 L232 209 L250 209 L335 209"], outputRelay("YC2-1", "远传出口继电器 2", 241, 209, "第二路远传继电器可独立组态，用于另一告警、闭锁或联动信号。"))
    ]
  },
  {
    id: "31-18", title: "1-1n 信号回路图", shortTitle: "信号回路", entityCount: 960,
    note: "1-1n 远动、录波、开关位置及操作箱信号回路。",
    filters: [{ id: "remote", label: "远动" }, { id: "breaker", label: "开关" }, { id: "record", label: "录波" }],
    loops: [
      pageLoop(1, "装置故障远动", "装置故障1", "remote", "1n 装置自检故障接点送往远动系统，形成装置故障告警。", ["装置故障 1", "远动开入"], ["M45 266 L102 266 L160 266"]),
      pageLoop(2, "运行异常远动", "运行异常1", "remote", "装置运行异常接点送往远动系统，提示通道、采样或运行状态异常。", ["运行异常 1", "远动开入"], ["M45 258 L102 258 L160 258"]),
      pageLoop(3, "保护动作远动", "保护动作1", "remote", "保护动作总信号接点送往远动系统，供监控后台告警和事件记录。", ["保护动作 1", "远动开入"], ["M45 250 L102 250 L160 250"]),
      pageLoop(4, "重合闸远动", "重合闸1", "remote", "重合闸动作信号接点送往远动系统，记录自动重合闸执行情况。", ["重合闸 1", "远动开入"], ["M45 242 L102 242 L160 242"]),
      pageLoop(5, "过负荷告警远动", "过负荷", "remote", "过负荷告警接点送往远动系统，提示线路负荷超过设定门槛。", ["过负荷告警", "远动开入"], ["M45 218 L102 218 L160 218"]),
      pageLoop(6, "通道故障远动", "通道故障", "remote", "光纤差动通道故障接点送往远动系统，提示纵联通信不可用。", ["通道故障", "远动开入"], ["M45 210 L102 210 L160 210"]),
      pageLoop(7, "合后状态", "HH", "breaker", "操作箱合后继电器接点输出合后状态，供保护逻辑识别人工或遥控合闸后的状态。", ["合后", "操作箱", "1n 开入"], ["M236 266 L292 266 L348 266"]),
      pageLoop(8, "断路器合位信号", "HW", "breaker", "合位辅助接点输出断路器合位状态，送至保护及监控回路。", ["合位", "断路器辅助接点"], ["M236 258 L292 258 L348 258"]),
      pageLoop(9, "断路器跳位信号", "TW", "breaker", "跳位辅助接点输出断路器分位状态，送至保护及监控回路。", ["跳位", "断路器辅助接点"], ["M236 250 L292 250 L348 250"]),
      pageLoop(10, "闭锁重合闸信号", "BSCH", "breaker", "操作箱闭锁重合闸接点送入保护装置，禁止不允许的重合操作。", ["闭锁重合闸", "操作箱", "1n 开入"], ["M236 242 L292 242 L348 242"]),
      pageLoop(11, "手跳信号", "ST", "breaker", "手动或遥控跳闸形成手跳识别信号，使装置区分保护跳闸并闭锁重合闸。", ["手跳", "STJ", "1n 开入"], ["M236 226 L292 226 L348 226"]),
      pageLoop(12, "闭锁合闸信号", "BSH", "breaker", "机构闭锁合闸接点反映气压、储能等合闸条件是否满足。", ["闭锁合闸", "机构接点"], ["M45 186 L102 186 L160 186"]),
      pageLoop(13, "闭锁跳闸信号", "BST", "breaker", "机构闭锁跳闸接点反映跳闸回路外部闭锁状态。", ["闭锁跳闸", "机构接点"], ["M45 178 L102 178 L160 178"]),
      pageLoop(14, "控制回路断线", "KHDX", "breaker", "操作电源或跳合闸监视异常时输出控制回路断线信号。", ["控制回路断线", "操作箱"], ["M45 170 L102 170 L160 170"]),
      pageLoop(15, "事故音响", "SGYX", "breaker", "断路器事故跳闸时输出事故音响接点，启动站内事故告警。", ["事故音响", "信号母线"], ["M45 162 L102 162 L160 162"]),
      pageLoop(16, "操作箱合位", "HWJ", "breaker", "操作箱合位继电器接点输出合位监视信号。", ["HWJ", "合位信号"], ["M45 154 L102 154 L160 154"]),
      pageLoop(17, "操作箱跳位", "TWJ", "breaker", "操作箱跳位继电器接点输出跳位监视信号。", ["TWJ", "跳位信号"], ["M45 146 L102 146 L160 146"]),
      pageLoop(18, "电压切换电源消失", "QHSD", "breaker", "电压切换箱失电时输出告警接点，提示 PT 自动切换功能不可用。", ["切换电源消失", "切换箱"], ["M45 118 L102 118 L160 118"]),
      pageLoop(19, "ⅠⅡ母同时动作", "QHYC", "breaker", "两组母线切换继电器同时动作时输出异常信号，提示切换状态冲突。", ["ⅠⅡ母同时动作", "切换异常"], ["M45 102 L102 102 L160 102"]),
      pageLoop(20, "装置故障录波", "装置故障2", "record", "装置故障第二组接点送往故障录波器，记录故障时刻的装置状态。", ["装置故障 2", "故障录波器"], ["M236 194 L292 194 L348 194"]),
      pageLoop(21, "运行异常录波", "运行异常2", "record", "运行异常第二组接点送往录波器，辅助分析保护未正常运行的原因。", ["运行异常 2", "故障录波器"], ["M236 186 L292 186 L348 186"]),
      pageLoop(22, "保护动作录波", "保护动作2", "record", "保护动作第二组接点启动或标记录波，形成动作时序依据。", ["保护动作 2", "故障录波器"], ["M236 178 L292 178 L348 178"]),
      pageLoop(23, "重合闸录波", "重合闸2", "record", "重合闸第二组接点送往录波器，记录重合命令及结果。", ["重合闸 2", "故障录波器"], ["M236 170 L292 170 L348 170"]),
      pageLoop(24, "过负荷告警录波", "过负荷2", "record", "过负荷第二组接点送往录波器，用于关联负荷与保护事件。", ["过负荷告警 2", "故障录波器"], ["M236 146 L292 146 L348 146"]),
      pageLoop(25, "通道故障录波", "通道故障2", "record", "通道故障第二组接点送往录波器，记录差动通信异常时段。", ["通道故障 2", "故障录波器"], ["M236 138 L292 138 L348 138"])
    ]
  }
];

// V10：依据 ZWCAD 对辅助接线图、端子图、空开图和压板图的交叉校核，
// 重新约束电源边界、流向、开入判断元件及可操作图元。辅助图只用于校核，不加入页面列表。
const pageFor = id => additionalDrawingPages.find(page => page.id === id);

{
  const page = pageFor("31-07");
  const loops = page.loops;
  const dk11 = interactiveDevice("dk11", "1-1DK", "1n 装置及开入电源空开", "breaker", 78, 260);
  const dk11Relay = outputRelay("1-1DK", "装置与开入电源空开", 78, 260,
    "1-1DK 为双极直流空开。合闸时正、负电源同时由左向右送出；分闸时两极电位只到达空开上游，不再进入装置及开入、切换支路。", "双极合闸");

  Object.assign(loops[0], {
    tag: "1-1DK",
    summary: "直流正、负电源从图纸左侧进入，经 1-1DK 双极空开由左向右送入 1n 保护装置。空开分开时，两极电位均只到达空开上游。",
    paths: ["M44 264 L78 264 L148 264", "M44 256 L78 256 L148 256"],
    relay: dk11Relay,
    device: dk11,
    flow: { direction: "forward", gateIndexes: [0, 1] }
  });
  Object.assign(loops[1], {
    tag: "1-1DK / 1-1QD",
    summary: "开入正、负电源取自 1-1DK 下游，分别由 1-1QD:3、1-1QD:19 向右送往开入回路；该支路与装置电源共用 1-1DK，并不存在独立的 1-2DK。",
    components: ["1-ZD:1/4（左侧电源）", "1-1DK", "1-1QD:3（开入+）", "1-1QD:19（开入-）"],
    paths: ["M44 264 L78 264 L112 264 L112 228 L148 228", "M44 256 L78 256 L120 256 L120 220 L148 220"],
    relay: dk11Relay,
    device: dk11,
    flow: { direction: "forward", gateIndexes: [0, 1] }
  });
  Object.assign(loops[2], {
    tag: "1-1DK / 1-7QD",
    summary: "电压切换正、负电源取自 1-1DK 下游，分别由 1-7QD:1、1-7QD:10 向右送往切换回路；图中不存在独立的 1-3DK。",
    components: ["1-ZD:1/4（左侧电源）", "1-1DK", "1-7QD:1（切换+）", "1-7QD:10（切换-）"],
    paths: ["M44 264 L78 264 L112 264 L112 208 L148 208", "M44 256 L78 256 L120 256 L120 200 L148 200"],
    relay: dk11Relay,
    device: dk11,
    flow: { direction: "forward", gateIndexes: [0, 1] }
  });
  Object.assign(loops[3], {
    paths: ["M44 188 L78 188 L148 188", "M44 180 L78 180 L148 180"],
    flow: { direction: "forward", gateIndexes: [0, 1] }
  });
  Object.assign(loops[4], {
    tag: "KG",
    components: ["JD:1（L）", "KG", "LAMP", "JD:4（N）"],
    paths: ["M260 264 L304 264 L344 264", "M260 252 L344 252"],
    relay: outputRelay("KG", "柜内照明开关", 304, 264,
      "KG 合上后，交流相线由左向右送至柜内照明灯；中性线保持直接接通。KG 分开时相线只到达开关处。", "开关闭合"),
    device: interactiveDevice("kg", "KG", "柜内照明开关", "switch", 304, 264),
    flow: { direction: "forward", gateIndexes: [0] }
  });
  Object.assign(loops[5], {
    tag: "PRT",
    summary: "交流 L、N 从左侧 JD:2、JD:5 直接送入 PRT 打印机电源端。原图该支路未画单独空开，因此只展示由左向右的供电路径。",
    components: ["JD:2（L）", "JD:5（N）", "PRT 打印机"],
    paths: ["M260 232 L344 232", "M260 222 L344 222"],
    flow: { direction: "forward" }
  });
  delete loops[5].relay;
  delete loops[5].device;
}

{
  const page = pageFor("31-09");
  const configs = [
    { paths: ["M78 264 L112 264 L176 264 L228 264 L240 264 L340 264"], code: "ZJ1-1", target: [112, 264], name: "Ⅰ母 A 相电压切换接点", principle: "左侧 1-7UD:1 为电压源。ZJ1-1 动作后接通 A 相电压，再经 1-1UK 送入装置 Ua 采样通道。" },
    { paths: ["M78 248 L112 248 L176 248 L228 248 L240 248 L340 248"], code: "ZJ1-2", target: [112, 248], name: "Ⅰ母 B 相电压切换接点", principle: "左侧 1-7UD:2 为电压源。ZJ1-2 动作后接通 B 相电压，再经 1-1UK 送入装置 Ub 采样通道。" },
    { paths: ["M78 232 L112 232 L176 232 L228 232 L240 232 L340 232"], code: "ZJ2-1", target: [112, 232], name: "母线 C 相电压切换接点", principle: "左侧 1-7UD:3 为电压源。ZJ2-1 接点切换后，C 相电压由左向右送入 Uc 采样通道。" },
    { paths: ["M78 160 L216 160 L216 232 L258 232 L296 232 L340 232"], code: "Ux", target: [296, 232], name: "线路抽取电压输入元件", principle: "线路抽取电压从左侧 1-7UD:32 引入，在装置 Ux 输入元件处形成采样量，供同期或无压判别。" },
    { paths: ["M258 180 L296 180 L340 180"], code: "Ia", target: [296, 180], name: "A 相电流输入元件", principle: "1-1ID:1 为该支路左侧源端，电流由左向右通过 Ia 输入元件，再由 1-1ID:5 引出。" },
    { paths: ["M258 172 L296 172 L340 172"], code: "Ib", target: [296, 172], name: "B 相电流输入元件", principle: "1-1ID:2 为该支路左侧源端，电流由左向右通过 Ib 输入元件，再由 1-1ID:6 引出。" },
    { paths: ["M258 164 L296 164 L340 164"], code: "Ic", target: [296, 164], name: "C 相电流输入元件", principle: "1-1ID:3 为该支路左侧源端，电流由左向右通过 Ic 输入元件，再由 1-1ID:7 引出。" },
    { paths: ["M258 156 L296 156 L340 156"], code: "I0", target: [296, 156], name: "零序电流输入元件", principle: "1-1ID:4 为该支路左侧源端，电流由左向右通过 I0 输入元件，再由 1-1ID:8 引出。" }
  ];
  configs.forEach((config, index) => Object.assign(page.loops[index], {
    paths: config.paths,
    relay: outputRelay(config.code, config.name, ...config.target, config.principle, "输入元件响应"),
    flow: { direction: "forward" }
  }));
}

{
  const page = pageFor("31-11");
  const positiveLeft = rowY => `M37 275 L37 ${rowY} L64 ${rowY} L82 ${rowY} L138 ${rowY}`;
  const negative21 = rowY => `M123 216 L152 216 L152 ${rowY} L138 ${rowY}`;
  const positiveRightPlate = rowY => `M225 275 L225 ${rowY} L252 ${rowY} L270 ${rowY} L326 ${rowY}`;
  const positiveRightDirect = rowY => `M233 264 L233 ${rowY} L286 ${rowY} L326 ${rowY}`;
  const negative20 = rowY => `M256 142 L338 142 L338 ${rowY} L326 ${rowY}`;
  const configs = [
    { y: 264, target: [138, 264], paths: [positiveLeft(264), negative21(264)], source: "1-1QD:3（正）", negative: "1-1QD:21（负）" },
    { y: 256, target: [138, 256], paths: [positiveLeft(256), negative21(256)], source: "1-1QD:3（正）", negative: "1-1QD:21（负）" },
    { y: 240, target: [138, 240], paths: ["M37 275 L37 240 L72 240 L80 240 L138 240", "M69 232 L152 232 L152 240 L138 240"], source: "1-1QD:3（正）", negative: "1-1QD:19（负）" },
    { y: 264, target: [326, 264], paths: [positiveRightPlate(264), negative20(264)], source: "1-1QD:4 / 1-1QD:5（正）", negative: "1-1QD:20（负）" },
    { y: 256, target: [326, 256], paths: [positiveRightPlate(256), negative20(256)], source: "1-1QD:4 / 1-1QD:5（正）", negative: "1-1QD:20（负）" },
    { y: 248, target: [326, 248], paths: [positiveRightPlate(248), negative20(248)], source: "1-1QD:4 / 1-1QD:5（正）", negative: "1-1QD:20（负）" },
    { y: 240, target: [326, 240], paths: [positiveRightPlate(240), negative20(240)], source: "1-1QD:4 / 1-1QD:5（正）", negative: "1-1QD:20（负）" },
    { y: 216, target: [326, 216], paths: [positiveRightDirect(216), negative20(216)], source: "1-1QD:5（正）", negative: "1-1QD:20（负）" },
    { y: 200, target: [326, 200], paths: [positiveRightDirect(200), negative20(200)], source: "1-1QD:5（正）", negative: "1-1QD:20（负）" },
    { y: 192, target: [326, 192], paths: [positiveRightDirect(192), negative20(192)], source: "1-1QD:5（正）", negative: "1-1QD:20（负）" },
    { y: 184, target: [326, 184], paths: [positiveRightDirect(184), negative20(184)], source: "1-1QD:5（正）", negative: "1-1QD:20（负）" },
    { y: 176, target: [326, 176], paths: [positiveRightDirect(176), negative20(176)], source: "1-1QD:5（正）", negative: "1-1QD:20（负）" },
    { y: 168, target: [326, 168], paths: [positiveRightDirect(168), negative20(168)], source: "1-1QD:5（正）", negative: "1-1QD:20（负）" },
    { y: 160, target: [326, 160], paths: [positiveRightDirect(160), negative20(160)], source: "1-1QD:5（正）", negative: "1-1QD:20（负）" }
  ];
  configs.forEach((config, index) => {
    const loop = page.loops[index];
    const inputCode = index < 3 ? ["19", "17", "18"][index] : ["01", "03", "05", "07", "11", "15", "17", "13", "09", "02", "04"][index - 3];
    Object.assign(loop, {
      paths: config.paths,
      components: [config.source, ...(loop.device ? [loop.device.code] : []), `开入判断 ${inputCode}`, config.negative],
      relay: outputRelay(`IN-${inputCode}`, `${loop.name}开入判断元件`, ...config.target,
        `正电由 ${config.source.replace("（正）", "")} 侧送入，负电由 ${config.negative.replace("（负）", "")} 侧返回；两侧在装置矩形框内的开入判断元件汇合后，装置才判定该开入有效。`, "开入判据置 1"),
      flow: { direction: "forward", ...(loop.device ? { gateIndexes: [0] } : {}) }
    });
  });
  Object.assign(page.loops[0].device, { target: [82, 264] });
  Object.assign(page.loops[1].device, { target: [82, 256] });
  Object.assign(page.loops[2].device, { target: [80, 240] });
  Object.assign(page.loops[3].device, { target: [270, 264] });
  Object.assign(page.loops[4].device, { target: [270, 256] });
  Object.assign(page.loops[5].device, { target: [270, 248] });
  Object.assign(page.loops[6].device, { target: [270, 240] });
}

{
  const page = pageFor("31-17");
  const ys = [264, 256, 248, 240, 232, 224, 216, 208];
  page.loops.forEach((loop, index) => {
    const y = ys[index];
    loop.paths = [`M167 ${y} L194 ${y} L218 ${y} L241 ${y} L266 ${y} L292 ${y} L335 ${y}`];
    loop.flow = { direction: "forward" };
  });
  const plates = [
    { code: "1-1CLP1", id: "clp1", label: "跳闸出口压板", y: 264 },
    { code: "1-1CLP3", id: "clp3", label: "重合闸出口压板", y: 256 },
    { code: "1-1CLP2", id: "clp2", label: "跳闸备用出口压板", y: 248 }
  ];
  plates.forEach((plate, index) => {
    const loop = page.loops[index];
    loop.device = interactiveDevice(plate.id, plate.code, plate.label, "plate", 292, plate.y);
    loop.flow = { direction: "forward", gateIndexes: [0], relayIndependent: true };
    loop.components = ["1-4QD:3（正电源）", loop.relay.code, plate.code, ...loop.components.filter(item => !item.includes(loop.relay.code))];
  });
}

{
  const page = pageFor("31-18");
  const configs = [
    ["BSJ-1", 92, 264, "装置自检故障时，BSJ-1 接点切换并送出装置故障远动信号。"],
    ["BJJ-1", 92, 256, "装置运行异常时，BJJ-1 接点切换并送出运行异常远动信号。"],
    ["SIG1-1", 92, 248, "保护动作后，SIG1-1 接点切换并送出保护动作远动信号。"],
    ["SIG2-1", 92, 240, "重合闸动作后，SIG2-1 接点切换并送出重合闸远动信号。"],
    ["GFH-1", 92, 216, "过负荷判据成立时，GFH-1 接点切换并送出告警。"],
    ["TDGZ1-1", 90, 208, "差动通道异常时，TDGZ1-1 接点切换并送出通道故障告警。"],
    ["HHJ", 285, 264, "HHJ 反映断路器合闸后的保持状态，并把合后状态送往保护配合回路。"],
    ["HWJ", 285, 256, "HWJ 反映断路器合位，其接点切换后输出合位信号。"],
    ["TWJ", 285, 248, "TWJ 反映断路器跳位，其接点切换后输出跳位信号。"],
    ["TJR / STJ", 285, 240, "TJR、STJ 接点用于闭锁重合闸，任一相应条件成立都会改变该信号支路。"],
    ["STJ", 285, 224, "手跳命令使 STJ 动作，其接点切换后输出手跳识别信号。"],
    ["BSH", 93, 184, "BSH 接点反映机构闭锁合闸状态。"],
    ["BST", 93, 176, "BST 接点反映机构闭锁跳闸状态。"],
    ["TWJ / HWJ", 93, 168, "TWJ 与 HWJ 接点组合用于判断控制回路断线。"],
    ["TWJ / HHJ", 93, 160, "TWJ 与 HHJ 接点组合用于形成事故音响信号。"],
    ["HWJ", 93, 152, "HWJ 接点输出操作箱合位监视信号。"],
    ["TWJ", 93, 144, "TWJ 接点输出操作箱跳位监视信号。"],
    ["ZJ10-2 / ZJ5-2", 91, 116, "切换继电器接点组合用于判断电压切换电源消失。"],
    ["ZJ10 / ZJ5 / ZJ11 / ZJ12", 91, 102, "两组切换继电器接点组合用于识别两段母线切换同时动作。"],
    ["BSJ-2", 284, 192, "BSJ-2 为装置故障第二组接点，用于故障录波。"],
    ["BJJ-2", 284, 184, "BJJ-2 为运行异常第二组接点，用于故障录波。"],
    ["SIG1-2", 284, 176, "SIG1-2 为保护动作第二组接点，用于标记录波。"],
    ["SIG2-2", 284, 168, "SIG2-2 为重合闸第二组接点，用于记录重合动作。"],
    ["GFH-2", 284, 144, "GFH-2 为过负荷告警第二组接点，用于故障录波。"],
    ["TDGZ1-2", 282, 136, "TDGZ1-2 为通道故障第二组接点，用于故障录波。"]
  ];
  const ys = [264, 256, 248, 240, 216, 208, 264, 256, 248, 240, 224, 184, 176, 168, 160, 152, 144, 116, 102, 192, 184, 176, 168, 144, 136];
  configs.forEach((config, index) => {
    const [code, x, y, principle] = config;
    const loop = page.loops[index];
    const leftGroup = index < 6 || (index >= 11 && index < 19);
    loop.paths = [`M${leftGroup ? 45 : 236} ${ys[index]} L${x} ${ys[index]} L${leftGroup ? 160 : 348} ${ys[index]}`];
    loop.relay = outputRelay(code, `${loop.name}信号继电器`, x, y, principle, "触点切换");
    loop.flow = { direction: "forward" };
  });
}
