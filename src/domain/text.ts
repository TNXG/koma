/** 文本清洗：把教务接口里的换行/连续空白压成单空格后 trim；非字符串一律空串 */
export function cleanText(value: unknown): string {
  return typeof value === "string"
    ? value.replace(/\s+/g, " ").trim()
    : "";
}
