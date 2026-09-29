package com.somepro.application.support;

/**
 * 业务单号生成小工具（应用层共享）。
 *
 * 形如 FM-2026-0001 / ET-2026-000001：前缀-年份-定长顺序号。
 * 序号以库里该年已占用的最大号为底 +1；库里可能躺着早先录入的数据，
 * 最大号按字符串可直接比较（前缀同年份、定宽补零，字典序即数值序）。
 * 超出定宽时自动加长（理论上极端情况），不截断、不撞号。
 */
public final class BizNoGenerator {

    private BizNoGenerator() {
    }

    /** 该年还没有任何单据时的首个号：序号 1。 */
    public static String first(String prefix, int year, int width) {
        return format(prefix, year, 1, width);
    }

    /**
     * 基于该年现有最大号算下一个号。
     *
     * @param maxNo 库里查到的最大单号（含历史软删行），形如 FM-2026-0001
     */
    public static String next(String prefix, int year, String maxNo, int width) {
        long seq = 1;
        if (maxNo != null) {
            int dash = maxNo.lastIndexOf('-');
            if (dash >= 0 && dash + 1 < maxNo.length()) {
                try {
                    seq = Long.parseLong(maxNo.substring(dash + 1).trim()) + 1;
                } catch (NumberFormatException ignore) {
                    // 历史号不符合定宽数字约定时退化为从 1 开始；唯一键仍是最后防线
                    seq = 1;
                }
            }
        }
        return format(prefix, year, seq, width);
    }

    private static String format(String prefix, int year, long seq, int width) {
        StringBuilder sb = new StringBuilder(prefix).append('-').append(year).append('-');
        String n = Long.toString(seq);
        for (int i = n.length(); i < width; i++) {
            sb.append('0');
        }
        return sb.append(n).toString();
    }
}
