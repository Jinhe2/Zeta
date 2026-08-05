/**
 * 业务系统（ct-screen-monitor）。
 *
 * <p>业务库保存教学系统自己的运行数据和展示配置；基础屏柜、装置、逻辑框图、端子、压板等台账仍来自
 * 只读的 {@code com.zeta.screen} / {@code ct-screen}。跨库关系不使用 JPA 关联，统一在业务实体中保存
 * 对应的 screen 主键字段，例如 {@code screenCabinetId}、{@code screenDeviceId}、{@code logicDiagramId}。
 *
 * <p>当前分层：
 * <ul>
 *   <li>{@code entities} — 业务库 Entity、Repository、实体枚举及其近旁 DTO</li>
 *   <li>{@code service} — 业务编排与跨库聚合逻辑</li>
 *   <li>{@code controller} — HTTP API 控制器</li>
 *   <li>{@code auth} — 登录鉴权、JWT、刷新令牌</li>
 *   <li>{@code storage} — 图片、视频、学习资料等本地文件存储</li>
 *   <li>{@code media} — 媒体类型等非实体公共定义</li>
 * </ul>
 *
 * <p>与 screen 实体的主要对应关系：
 * <ul>
 *   <li>{@code entities.binding.CabinetBinding} — 平板绑定到 {@code screen.cabinet.Cabinet}</li>
 *   <li>{@code entities.cabinetdisplay.CabinetDisplayItem} — 屏柜学习图，引用 {@code screen.cabinet.Cabinet}</li>
 *   <li>{@code entities.cognitiondevice.CognitionDevice} — 学习图上的认知区域，可引用 {@code screen.ieddevice.Device}</li>
 *   <li>{@code entities.devicedisplay.DeviceDisplayItem} — 认知区域下的教学内容</li>
 *   <li>{@code entities.logiclearning.LogicLearningConfig} — 保护逻辑展示配置，引用 {@code screen.logicdiagram.ProtectionLogic}</li>
 *   <li>{@code entities.logicnodecognition.LogicNodeCognitionItem} — 保护逻辑节点教学内容，节点来自逻辑框图 JSON</li>
 *   <li>{@code entities.drawinglearning.DrawingGroup} — 图纸学习内容，引用 {@code screen.cabinet.Cabinet}</li>
 *   <li>{@code entities.learningresource.LearningResource} — 学习资料，可限定到 {@code screen.cabinet.Cabinet}</li>
 *   <li>{@code entities.monitor.MonitorTask} — 实验监控任务，引用 screen IED 装置和保护逻辑</li>
 *   <li>{@code entities.snapshot.LogicSnapshot} — 学员实验断面，引用用户和 screen 保护逻辑</li>
 * </ul>
 */
package com.zeta.business;
