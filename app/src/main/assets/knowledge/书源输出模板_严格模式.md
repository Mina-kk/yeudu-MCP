# Legado书源输出模板（严格模式）

> **版本**: 2.0（严格版）
> **更新时间**: 2025-01-06
> **重要**: 本模板基于真实书源库（134个书源）和核心视图工具提取，必须严格遵守！
> **禁止**: 禁止编造、禁止Mock、禁止偏离模板格式！

---

## 📋 目录

1. [输出规范](#1-输出规范)
2. [标准书源模板](#2-标准书源模板)
3. [五种真实模板](#3-五种真实模板)
4. [字段验证清单](#4-字段验证清单)
5. [常见错误示例](#5-常见错误示例)
6. [输出示例](#6-输出示例)

---

## 1. 输出规范

### 1.1 必须遵守的原则

| 原则 | 说明 | 违规后果 |
|------|------|----------|
| **JSON格式** | 必须是标准JSON，可直接导入Legado | ❌ 无法导入 |
| **数组格式** | 必须输出JSON数组格式 `[...]` | ❌ 无法导入 |
| **必填字段** | 必须包含所有必填字段 | ❌ 导入失败 |
| **真实分析** | 所有规则必须基于真实HTML分析 | ❌ 规则无效 |
| **禁止Mock** | 禁止使用模拟数据 | ❌ 数据错误 |
| **参考模板** | 必须参考真实模板格式 | ❌ 格式错误 |

### 1.2 必填字段清单

```js
{
  "bookSourceUrl": "必填",        // 书源地址
  "bookSourceName": "必填",       // 书源名称
  "searchUrl": "必填",            // 搜索URL
  "ruleSearch": {                 // 搜索规则
    "bookList": "必填",           // 书籍列表选择器
    "name": "必填",               // 书名提取规则
    "author": "可选",              // 作者提取规则
    "coverUrl": "可选",           // 封面提取规则
    "bookUrl": "必填"             // 书籍URL提取规则
  },
  "ruleToc": {                    // 目录规则
    "chapterList": "必填",        // 章节列表选择器
    "chapterName": "必填",        // 章节名提取规则
    "chapterUrl": "必填"          // 章节URL提取规则
  },
  "ruleContent": {                // 正文规则
    "content": "必填"             // 正文内容提取规则
  }
}
```

### 1.3 输出格式要求

```js
[
  {
    "bookSourceUrl": "https://www.example.com",
    "bookSourceName": "示例书源",
    ...
  }
]
```

**注意**：
- ✅ 最外层必须是数组 `[...]`
- ✅ 数组内可以包含一个或多个书源对象
- ✅ 每个书源对象必须符合标准结构
- ❌ 不能直接输出对象（必须是数组）

---

## 2. 标准书源模板

### 2.1 完整模板结构

```js
{
  "bookSourceName": "书源名称",
  "bookSourceUrl": "https://www.example.com",
  "bookSourceGroup": "分组名",
  "bookSourceType": 0,
  "bookSourceComment": "书源说明",
  "loginUrl": "",
  "loginUi": "",
  "loginCheckJs": "",
  "concurrentRate": "",
  "header": "",
  "searchUrl": "/search?q={{key}}",
  "exploreUrl": "",
  "enabled": true,
  "enabledExplore": true,
  "weight": 0,
  "customOrder": 0,
  "lastUpdateTime": 0,
  "ruleSearch": {
    "bookList": "选择器",
    "name": "选择器@提取类型##正则##",
    "author": "选择器@提取类型##正则##",
    "kind": "选择器@提取类型##正则##",
    "wordCount": "选择器@提取类型##正则##",
    "lastChapter": "选择器@提取类型##正则##",
    "intro": "选择器@提取类型##正则##",
    "coverUrl": "选择器@提取类型",
    "bookUrl": "选择器@提取类型"
  },
  "ruleBookInfo": {
    "name": "选择器@提取类型##正则##",
    "author": "选择器@提取类型##正则##",
    "kind": "选择器@提取类型##正则##",
    "wordCount": "选择器@提取类型##正则##",
    "lastChapter": "选择器@提取类型##正则##",
    "intro": "选择器@提取类型##正则##",
    "coverUrl": "选择器@提取类型",
    "tocUrl": ""
  },
  "ruleToc": {
    "chapterList": "选择器",
    "chapterName": "选择器@提取类型##正则##",
    "chapterUrl": "选择器@提取类型##正则##",
    "preUpdateJs": "",
    "updateJs": "",
    "nextTocUrl": ""
  },
  "ruleContent": {
    "content": "选择器@提取类型##正则##",
    "nextContentUrl": "",
    "webJs": "",
    "sourceRegex": "",
    "replaceRegex": ""
  },
  "ruleExplore": {
    "exploreList": "选择器",
    "title": "选择器@提取类型##正则##",
    "name": "选择器@提取类型##正则##",
    "author": "选择器@提取类型##正则##",
    "kind": "选择器@提取类型##正则##",
    "wordCount": "选择器@提取类型##正则##",
    "lastChapter": "选择器@提取类型##正则##",
    "intro": "选择器@提取类型##正则##",
    "coverUrl": "选择器@提取类型",
    "bookUrl": "选择器@提取类型"
  }
}
```

### 2.2 最小化模板（仅必填字段）

```js
{
  "bookSourceName": "书源名称",
  "bookSourceUrl": "https://www.example.com",
  "searchUrl": "/search?q={{key}}",
  "ruleSearch": {
    "bookList": ".book-item",
    "name": ".title@text",
    "author": ".author@text",
    "coverUrl": "img@src",
    "bookUrl": "a@href"
  },
  "ruleToc": {
    "chapterList": "#chapter-list li",
    "chapterName": "a@text",
    "chapterUrl": "a@href"
  },
  "ruleContent": {
    "content": "#content@html"
  }
}
```

---

## 3. 五种真实模板

### 3.1 模板1：69书吧（Default+XPath）

**适用场景**：典型小说站，使用POST请求，支持GBK编码

```js
{
  "bookSourceName": "69书吧",
  "bookSourceUrl": "https://www.69shuba.com",
  "bookSourceType": 0,
  "searchUrl": "/modules/article/search.php,{\"method\":\"POST\",\"body\":\"searchkey={{key}}&searchtype=all\",\"charset\":\"gbk\"}",
  "ruleSearch": {
    "bookList": "class.newbox@tag.li",
    "name": "tag.a.0@text",
    "author": "tag.span.-1@text##.*：",
    "bookUrl": "tag.a.0@href",
    "coverUrl": "tag.img@src"
  },
  "ruleBookInfo": {
    "name": "class.booknav2@tag.h1@text",
    "author": "class.booknav2@tag.a.0@text",
    "coverUrl": "class.bookimg2@tag.img@src",
    "intro": "class.navtxt@tag.p.-1@text",
    "kind": "class.booknav2@tag.a.1@text",
    "lastChapter": "class.qustime@tag.a@text"
  },
  "ruleToc": {
    "chapterList": "id.catalog@tag.li",
    "chapterName": "tag.a@text",
    "chapterUrl": "tag.a@href"
  },
  "ruleContent": {
    "content": "class.txtnav@html##<p>.*?</p>|<script[\\s\\S]*?</script>"
  }
}
```

**关键特点**：
- ✅ 使用POST请求
- ✅ body必须用String()类型（格式已处理）
- ✅ 支持GBK编码
- ✅ 使用Default+XPath语法

---

### 3.2 模板2：笔趣阁（Default推荐）

**适用场景**：笔趣阁类站点，使用Default语法（推荐）

```js
{
  "bookSourceName": "笔趣阁",
  "bookSourceUrl": "https://www.biquge.com",
  "bookSourceType": 0,
  "searchUrl": "/search.php?q={{key}}",
  "ruleSearch": {
    "bookList": "class.result-list@class.result-item",
    "name": "class.result-game-item-title-link@text",
    "author": "@css:.result-game-item-info-tag:nth-child(1)@text##作\\s*者：",
    "bookUrl": "class.result-game-item-title-link@href",
    "coverUrl": "class.result-game-item-pic@tag.img@src",
    "intro": "class.result-game-item-desc@text"
  },
  "ruleBookInfo": {
    "name": "id.info@tag.h1@text",
    "author": "@css:#info p:nth-child(1)@text##作.*?：",
    "coverUrl": "id.fmimg@tag.img@src",
    "intro": "id.intro@text",
    "lastChapter": "@css:#info p:nth-child(4) a@text"
  },
  "ruleToc": {
    "chapterList": "id.list@tag.dd@tag.a",
    "chapterName": "text",
    "chapterUrl": "href"
  },
  "ruleContent": {
    "content": "id.content@html##<script[\\s\\S]*?</script>|请收藏.*"
  }
}
```

**关键特点**：
- ✅ 使用Default语法（推荐）
- ✅ 简洁的选择器
- ✅ 复杂选择器使用@css前缀
- ✅ 正则表达式清理内容

---

### 3.3 模板3：起点中文网（JSONPath）

**适用场景**：API型书源，返回JSON数据

```js
{
  "bookSourceName": "起点中文网",
  "bookSourceUrl": "https://m.qidian.com",
  "bookSourceType": 0,
  "searchUrl": "https://m.qidian.com/majax/search/list?kw={{key}}&pageNum={{page}}",
  "ruleSearch": {
    "bookList": "$.data.records",
    "name": "$.bName",
    "author": "$.bAuth",
    "bookUrl": "https://m.qidian.com/book/{{$.bid}}",
    "coverUrl": "https://bookcover.yuewen.com/qdbimg/349573/{{$.bid}}/150",
    "intro": "$.desc",
    "kind": "$.cat"
  },
  "ruleBookInfo": {
    "tocUrl": "/ajax/book/category?bookId=@css:meta[property=\"og:url\"]@content##.*/",
    "init": "@json:$.data"
  },
  "ruleToc": {
    "chapterList": "$.data.vs[*].cs[*]",
    "chapterName": "$.cN",
    "chapterUrl": "https://m.qidian.com/book/{{$.bid}}/{{$.id}}"
  },
  "ruleContent": {
    "content": "$.data.content",
    "sourceRegex": "m\\.qidian\\.com/majax/chapter/getChapterInfo"
  }
}
```

**关键特点**：
- ✅ 使用JSONPath提取数据
- ✅ 返回JSON数据
- ✅ 使用`$.`语法提取字段
- ✅ 支持动态构建URL

---

### 3.4 模板4：新笔趣阁（XPath）

**适用场景**：需要使用XPath选择器的网站

```js
{
  "bookSourceName": "新笔趣阁",
  "bookSourceUrl": "https://www.xbiquge.la",
  "bookSourceType": 0,
  "searchUrl": "/search.php?keyword={{key}}",
  "ruleSearch": {
    "bookList": "//div[@class=\"result-item\"]",
    "name": "//h3/a/text()",
    "author": "//p[@class=\"result-game-item-info-tag\"][1]/span[2]/text()",
    "bookUrl": "//h3/a/@href",
    "coverUrl": "//img/@src",
    "intro": "//p[@class=\"result-game-item-desc\"]/text()"
  },
  "ruleBookInfo": {
    "name": "//div[@id=\"info\"]/h1/text()",
    "author": "//div[@id=\"info\"]/p[1]/a/text()",
    "coverUrl": "//div[@id=\"fmimg\"]/img/@src",
    "intro": "//div[@id=\"intro\"]/p/text()",
    "lastChapter": "//div[@id=\"info\"]/p[4]/a/text()"
  },
  "ruleToc": {
    "chapterList": "//div[@id=\"list\"]/dl/dd/a",
    "chapterName": "/text()",
    "chapterUrl": "/@href"
  },
  "ruleContent": {
    "content": "//div[@id=\"content\"]",
    "replaceRegex": "##一秒记住.*|##请收藏本站.*"
  }
}
```

**关键特点**：
- ✅ 使用XPath语法
- ✅ 使用`//`选择器
- ✅ 使用`[]`属性选择
- ✅ 使用`/`提取文本或属性

---

### 3.5 模板5：猫耳FM（有声书）

**适用场景**：有声书网站，需要WebView渲染

```js
{
  "bookSourceName": "猫耳FM",
  "bookSourceUrl": "https://www.missevan.com",
  "bookSourceType": 1,
  "searchUrl": "https://www.missevan.com/dramaapi/search?s={{key}}&page=1",
  "ruleSearch": {
    "bookList": "$.info.Datas",
    "name": "$.name",
    "author": "$.author",
    "bookUrl": "https://www.missevan.com/mdrama/drama/{{$.id}},{\"webView\":true}",
    "coverUrl": "$.cover",
    "intro": "$.abstract"
  },
  "ruleToc": {
    "chapterList": "@css:.scroll-list.btn-groups>a",
    "chapterName": "text",
    "chapterUrl": "href##$##,{\"webView\":true}"
  },
  "ruleContent": {
    "content": "https://static.missevan.com/{{//*[contains(@class,\"pld-sound-active\")]/@data-soundurl64}}",
    "sourceRegex": ".*\\.(mp3|m4a).*"
  }
}
```

**关键特点**：
- ✅ bookSourceType = 1（有声书）
- ✅ 需要WebView渲染（webView: true）
- ✅ 音频链接提取
- ✅ 混合使用JSONPath和XPath

---

## 4. 字段验证清单

### 4.1 必填字段验证

| 字段 | 是否必填 | 默认值 | 说明 |
|------|----------|--------|------|
| bookSourceUrl | ✅ 必填 | 无 | 书源地址，不能为空 |
| bookSourceName | ✅ 必填 | 无 | 书源名称，不能为空 |
| searchUrl | ✅ 必填 | 无 | 搜索URL，不能为空 |
| ruleSearch.bookList | ✅ 必填 | 无 | 书籍列表选择器 |
| ruleSearch.name | ✅ 必填 | 无 | 书名提取规则 |
| ruleSearch.bookUrl | ✅ 必填 | 无 | 书籍URL提取规则 |
| ruleToc.chapterList | ✅ 必填 | 无 | 章节列表选择器 |
| ruleToc.chapterName | ✅ 必填 | 无 | 章节名提取规则 |
| ruleToc.chapterUrl | ✅ 必填 | 无 | 章节URL提取规则 |
| ruleContent.content | ✅ 必填 | 无 | 正文内容提取规则 |

### 4.2 可选字段验证

| 字段 | 是否必填 | 默认值 | 说明 |
|------|----------|--------|------|
| bookSourceGroup | ❌ 可选 | "" | 书源分组 |
| bookSourceType | ❌ 可选 | 0 | 书源类型 |
| bookSourceComment | ❌ 可选 | "" | 书源说明 |
| ruleSearch.author | ❌ 可选 | "" | 作者提取规则 |
| ruleSearch.kind | ❌ 可选 | "" | 分类提取规则 |
| ruleSearch.wordCount | ❌ 可选 | "" | 字数提取规则 |
| ruleSearch.lastChapter | ❌ 可选 | "" | 最新章节提取规则 |
| ruleSearch.intro | ❌ 可选 | "" | 简介提取规则 |
| ruleSearch.coverUrl | ❌ 可选 | "" | 封面提取规则 |

### 4.3 规则格式验证

| 规则格式 | 正确示例 | 错误示例 |
|----------|----------|----------|
| Default语法 | `.title@text` | `@css:.title@text`（简单选择器不需要@css） |
| 复杂选择器 | `@css:.result-game-item-info-tag:nth-child(1)@text` | `.result-game-item-info-tag:nth-child(1)@text` |
| 正则表达式 | `.author@text##^作者：##` | `.author@text##作者：##`（缺少结束符） |
| XPath | `//div[@id="content"]` | `#content`（XPath格式错误） |
| JSONPath | `$.data.records` | `$..data.records`（语法错误） |

---

## 5. 常见错误示例

### 5.1 格式错误

❌ **错误1：缺少数组格式**
```js
{
  "bookSourceName": "书源名称",
  "bookSourceUrl": "https://www.example.com",
  ...
}
```
**问题**：最外层是对象，不是数组
**修正**：
```js
[
  {
    "bookSourceName": "书源名称",
    "bookSourceUrl": "https://www.example.com",
    ...
  }
]
```

---

❌ **错误2：缺少必填字段**
```js
{
  "bookSourceName": "书源名称",
  "bookSourceUrl": "https://www.example.com"
}
```
**问题**：缺少searchUrl和ruleSearch等必填字段
**修正**：
```js
{
  "bookSourceName": "书源名称",
  "bookSourceUrl": "https://www.example.com",
  "searchUrl": "/search?q={{key}}",
  "ruleSearch": {
    "bookList": ".book-item",
    "name": ".title@text",
    "bookUrl": "a@href"
  },
  ...
}
```

---

### 5.2 规则错误

❌ **错误3：正则表达式缺少结束符**
```js
{
  "author": ".author@text##^作者"
}
```
**问题**：正则表达式缺少`##`结束符
**修正**：
```js
{
  "author": ".author@text##^作者：##"
}
```

---

❌ **错误4：选择器语法错误**
```js
{
  "name": "@css:.title@text"
}
```
**问题**：简单选择器不需要@css前缀
**修正**：
```js
{
  "name": ".title@text"
}
```

---

### 5.3 数据错误

❌ **错误5：使用Mock数据**
```js
{
  "bookSourceUrl": "https://www.example.com",
  "bookSourceName": "示例书源",
  "searchUrl": "/search?q={{key}}",
  "ruleSearch": {
    "bookList": ".book-item",
    "name": ".title@text",
    "author": ".author@text",
    "coverUrl": "https://via.placeholder.com/150",
    "bookUrl": "/book/{{id}}"
  }
}
```
**问题**：使用Mock数据（https://via.placeholder.com/150）
**修正**：基于真实HTML分析，提取真实的封面URL

---

❌ **错误6：POST请求配置错误**
```js
{
  "searchUrl": "/search,method=POST,body=keyword={{key}}"
}
```
**问题**：缺少引号，格式错误
**修正**：
```js
{
  "searchUrl": "/search,{\"method\":\"POST\",\"body\":\"keyword={{key}}\"}"
}
```

---

## 6. 输出示例

### 6.1 标准输出示例

```js
[
  {
    "bookSourceName": "歌书网",
    "bookSourceUrl": "https://m.gashuw.com",
    "bookSourceType": 0,
    "searchUrl": "/s.php",
    "ruleSearch": {
      "bookList": ".hot_sale",
      "name": ".title@text",
      "author": ".author:first-child@text##.*作者：##",
      "kind": ".author:first-child@text##^[^|]*##",
      "lastChapter": ".author:last-child@text##.*更新：##",
      "bookUrl": "a@href",
      "coverUrl": ""
    },
    "ruleBookInfo": {
      "name": "#book-title@text",
      "author": "#author@text##作者：##",
      "coverUrl": "#cover img@src",
      "intro": "#intro@text"
    },
    "ruleToc": {
      "chapterList": "#chapter-list li",
      "chapterName": "a@text",
      "chapterUrl": "a@href"
    },
    "ruleContent": {
      "content": "#content@html"
    }
  }
]
```

---

### 6.2 多书源输出示例

```js
[
  {
    "bookSourceName": "69书吧",
    "bookSourceUrl": "https://www.69shuba.com",
    "bookSourceType": 0,
    "searchUrl": "/modules/article/search.php,{\"method\":\"POST\",\"body\":\"searchkey={{key}}&searchtype=all\",\"charset\":\"gbk\"}",
    "ruleSearch": {
      "bookList": "class.newbox@tag.li",
      "name": "tag.a.0@text",
      "author": "tag.span.-1@text##.*：",
      "bookUrl": "tag.a.0@href",
      "coverUrl": "tag.img@src"
    },
    "ruleToc": {
      "chapterList": "id.catalog@tag.li",
      "chapterName": "tag.a@text",
      "chapterUrl": "tag.a@href"
    },
    "ruleContent": {
      "content": "class.txtnav@html##<p>.*?</p>|<script[\\s\\S]*?</script>"
    }
  },
  {
    "bookSourceName": "笔趣阁",
    "bookSourceUrl": "https://www.biquge.com",
    "bookSourceType": 0,
    "searchUrl": "/search.php?q={{key}}",
    "ruleSearch": {
      "bookList": "class.result-list@class.result-item",
      "name": "class.result-game-item-title-link@text",
      "author": "@css:.result-game-item-info-tag:nth-child(1)@text##作\\s*者：",
      "bookUrl": "class.result-game-item-title-link@href",
      "coverUrl": "class.result-game-item-pic@tag.img@src",
      "intro": "class.result-game-item-desc@text"
    },
    "ruleToc": {
      "chapterList": "id.list@tag.dd@tag.a",
      "chapterName": "text",
      "chapterUrl": "href"
    },
    "ruleContent": {
      "content": "id.content@html##<script[\\s\\S]*?</script>|请收藏.*"
    }
  }
]
```

---

## 📚 附录

### A. 模板来源

- **来源1**：`assets/核心视图工具.txt` - 第42635-42850行
- **来源2**：`assets/真实书源模板库.txt` - 5个真实书源模板
- **来源3**：`assets/真实书源知识库.md` - 134个真实书源分析

### B. 参考文档

- Legado官方文档：https://github.com/gedoor/legado
- 书源开发指南：https://github.com/gedoor/legado/wiki/BookSourceRules
- JSOUP语法说明：详见《Legado书源规则：从入门到入土.md》

### C. 严格模式说明

本模板的"严格模式"意味着：
1. ✅ 必须严格按照模板格式输出
2. ✅ 必须包含所有必填字段
3. ✅ 必须使用真实的规则（基于HTML分析）
4. ✅ 必须使用JSON数组格式
5. ❌ 禁止编造规则
6. ❌ 禁止使用Mock数据
7. ❌ 禁止偏离模板格式

---

## ⚠️ 重要提醒

1. **输出格式**：必须是JSON数组格式 `[...]`
2. **必填字段**：必须包含所有必填字段
3. **真实分析**：所有规则必须基于真实HTML分析
4. **禁止Mock**：禁止使用模拟数据
5. **参考模板**：必须参考真实模板格式
6. **验证规则**：输出前必须验证规则语法
7. **测试导入**：建议在Legado中测试导入

---

*本文档基于核心视图工具和134个真实书源分析生成，必须严格遵守！*
