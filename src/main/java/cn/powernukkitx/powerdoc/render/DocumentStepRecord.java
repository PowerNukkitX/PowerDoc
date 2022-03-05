package cn.powernukkitx.powerdoc.render;

import cn.powernukkitx.powerdoc.Document;

/**
 * 文档渲染记录
 * 每次文档被{@link Step}渲染后都应当创建一个记录，并添加到被渲染的文档中
 */
public record DocumentStepRecord(String stepName, Document document) {
}
