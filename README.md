# plugin-friends
* 朋友圈管理插件, 支持在 Console 进行管理以及为主题端提供 `/friends` 页面路由。
* 提供对 RSS 链接的订阅功能，支持获取其订阅内容

![friends.png](https://api.minio.yyds.pink/lsky/img/2024/01/65a7a631243d0.png)

![gl.png](https://api.minio.yyds.pink/lsky/img/2024/01/65a7a7ad77384.png)

![Snipaste_2024-01-17_18-10-27.png](https://api.minio.yyds.pink/lsky/img/2024/01/65a7a7ae04f24.png)

## 使用方式
* 在应用市场下载并启用。
* 启用插件之后会在 Console 的左侧添加一个`朋友圈`的菜单项，点击即可进入`朋友圈`管理页面。

## 特性
* 内置模板，无需主题支持，但也可以通过主题自定义模板。

## 主题适配
目前此插件为主题端提供了 `/friends` 路由，模板为 `friends.html`，也提供了 Finder API，可以将瞬间列表渲染到任何地方。

## 模板变量
路由信息
* 模板路径：/templates/friends.html
* 访问路径：/friends

### 变量
* friends
* statistical

### 变量类型
* UrlContextListResult<[FriendPostVo](#FriendPostVo)>
* [StatisticalVo](#StatisticalVo)

#### 示例
```bash
<div>
  <ul th:with="stats = ${statistical}">
    <li th:text="${stats.friendsNum}"></li>
    <li th:text="${stats.activeNum}"></li>
    <li th:text="${stats.articleNum}"></li>
  </ul>
  <div th:each="friend : ${friends.items}" th:with="spec = ${friend.spec}">
    <a th:href="${spec.link}" target="_blank" th:text="${spec.title}"></a>
    <div>
      <img th:src="${spec.logo}" alt="avatar">
      <a th:href="${spec.url}" target="_blank">
        <span th:text="${spec.author}"></span>
      </a>
    </div>
  </div>
  <div th:if="${friends.hasPrevious() || friends.hasNext()}">
    <a th:href="@{${friends.prevUrl}}">
      <span>上一页</span>
    </a>
    <span th:text="${friends.page}"></span>
    <a th:href="@{${friends.nextUrl}}">
      <span>下一页</span>
    </a>
  </div>
</div>
```

## Finder API

### listAll()

#### 描述
获取全部订阅文章内容。

#### 参数
无

#### 返回值
List<[FriendPostVo](#FriendPostVo)>

#### 示例

```bash
<div>
  <div th:each="friend : ${friendFinder.listAll()}" th:with="spec = ${friend.spec}">
    <a th:href="${spec.link}" target="_blank" th:text="${spec.title}"></a>
    <div >
      <img th:src="${spec.logo}" alt="avatar">
      <a th:href="${spec.url}" target="_blank">
        <span th:text="${spec.author}"></span>
      </a>
    </div>
  </div>
</div>
```

### list(page, size)

#### 描述
根据分页参数获取订阅文章内容。

#### 参数
* page: int - 分页页码，从 1 开始
* size: int - 分页条数

#### 返回值
[ListResult<MomentVo>](#ListResult)

#### 示例

```bash
<th:block th:with="friends = ${friendFinder.list(1, 10)}">
    <div>
      <div th:each="friend : ${friends.items}" th:with="spec = ${friend.spec}">
        <a th:href="${spec.link}" target="_blank" th:text="${spec.title}"></a>
        <div >
          <img th:src="${spec.logo}" alt="avatar">
          <a th:href="${spec.url}" target="_blank">
            <span th:text="${spec.author}"></span>
          </a>
        </div>
      </div>
    </div>
    <div>
      <span th:text="${friends.page}"></span>
    </div>
</th:block>
```

### statistical()

#### 描述
订阅统计

#### 参数
无

#### 返回值
[StatisticalVo](#StatisticalVo)

#### 示例

```bash
<ul th:with="stats = ${friendFinder.statistical()}">
  <li th:text="${stats.friendsNum}"></li>
  <li th:text="${stats.activeNum}"></li>
  <li th:text="${stats.articleNum}"></li>
</ul>
```

## 类型定义
### FriendPostVo
```bash
{
  "metadata": {
    "name": "string",                                         // 唯一标识
    "generateName": "string",
    "version": 0,
    "creationTimestamp": "2024-01-16T16:13:17.925131783Z",    // 创建时间
  },
  "apiVersion": "friend.moony.la/v1alpha1",
  "kind": "FriendPost",
  "spec": {
    "url": "string",                                          // 作者链接
    "author": "string",                                       // 作者名称
    "logo": "string",                                         // 作者logo
    "title": "string",                                        // 标题
    "link": "string",                                         // 链接
    "description": "string",                                  // 内容
    "pubDate": "date",                                        // 同步时间
  }
}
```

### StatisticalVo

```bash
{
  "friendsNum": "Integer",                                     // 订阅数
  "activeNum": "Integer",                                      // 同步订阅成功数
  "articleNum": "Integer",                                     // 文章数
}
```

### ListResult

```bash
{
  "page": 0,                                   // 当前页码
  "size": 0,                                   // 每页条数
  "total": 0,                                  // 总条数
  "items": "List<#FriendPostVo>",              // 订阅文章列表数据
  "first": true,                               // 是否为第一页
  "last": true,                                // 是否为最后一页
  "hasNext": true,                             // 是否有下一页
  "hasPrevious": true,                         // 是否有上一页
  "totalPages": 0                              // 总页数
}
```

### UrlContextListResult

```bash
{
  "page": 0,                                   // 当前页码
  "size": 0,                                   // 每页条数
  "total": 0,                                  // 总条数
  "items": "List<#FriendPostVo>",              // 订阅文章列表数据
  "first": true,                               // 是否为第一页
  "last": true,                                // 是否为最后一页
  "hasNext": true,                             // 是否有下一页
  "hasPrevious": true,                         // 是否有上一页
  "totalPages": 0,                             // 总页数
  "prevUrl": "string",                         // 上一页链接
  "nextUrl": "string"                          // 下一页链接
}
```

## 开发环境

```bash
git clone git@github.com:chengzhongxue/plugin-friends.git

# 或者当你 fork 之后

git clone git@github.com:{your_github_id}/plugin-friends.git
```

```bash
cd path/to/plugin-friends
```

```bash
# macOS / Linux
./gradlew pnpmInstall

# Windows
./gradlew.bat pnpmInstall
```

```bash
# macOS / Linux
./gradlew haloServer

# Windows
./gradlew.bat haloServer
```

```bash
# macOS / Linux
./gradlew build

# Windows
./gradlew.bat build
```

修改 Halo 配置文件：

```yaml
halo:
  plugin:
    runtime-mode: development
    classes-directories:
      - "build/classes"
      - "build/resources"
    lib-directories:
      - "libs"
    fixedPluginPath:
      - "/path/to/plugin-friends"
```
