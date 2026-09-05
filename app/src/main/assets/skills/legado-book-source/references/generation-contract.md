# 书源工坊生成合同

根据规则和真实 HTML 证据输出一个闭合的 BookSource JSON。不要假设本机有 Python 调试器或 legado 源码树。

## 必须遵守

1. 选择器只能使用证据里出现过的 tag / id / class / meta。禁止编造站点专属结构，禁止把某个小说站的写法当成所有网站的模板。
2. `nextContentUrl` 只表示**同一章的下一页**（文案是「下一页」「下页」）。禁止写成「下一章」「下一回」。没有分页就留空。
3. `nextTocUrl` 只表示目录分页，不是下一章。
4. `chapterList` 必须指向章节列表容器，不要用导航、分类、页脚链接。
5. `@css:` 只允许写在一条规则的开头一次；`||` 后面不要再写 `@css:`。优先 `id.xxx@tag.a` / `class.xxx@tag.a`。
6. 搜索规则必须来自「搜索结果页」证据。首页搜索表单只能用来写 `searchUrl`（保留 hidden 字段和 `{{key}}`）。没有结果页证据时，不要猜 `bookList`。
7. `replaceRegex` 是全部正文页合并之后的清洗，必须写成 `##正则##`；禁止贪婪 `##...$##` 从第一页吃到章末。
8. 只输出一个 JSON 对象，不要 Markdown，不要长注释。
9. 有真实正文容器时必须优先 CSS/XPath（如 `#content@html`、`id.chapterContent@text`），禁止为了“通用兜底”遍历整页最大文本块。
10. 工坊内嵌 Rhino 已知不兼容 `org.jsoup.Jsoup.parse(...)` 的直接调用；不得生成这种写法。只有证据证明必须用 JS 且 CSS/XPath 无法表达时，才允许显式 `Packages.org.jsoup.Jsoup`，并必须让脚本返回值而不是只 `java.log`。

## 建议顺序

详情（书名/作者/封面/简介）→ 目录（章节列表）→ 正文（内容，必要时下一页）→ 搜索（结果列表）。站点没有的能力不要伪造。
