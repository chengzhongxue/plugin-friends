//默认数据
var fdata = {
  initnumber: 20,  //首次加载文章数
  stepnumber: 10,  //更多加载文章数
  error_img: 'https://sdn.geekzu.org/avatar/57d8260dfb55501c37dde588e7c3852c'
}
var article_num = '', sortNow = '', UrlNow = '', friends_num = ''
var container = document.getElementById('cf-container');


function getDate(pubDate) {
  var date = new Date(pubDate);
  // 将日期时间字符串转换为本地时间
  var localDate = new Date(date.toISOString().slice(0,10));
  // 将本地时间转换为 YYYY-MM-DD 格式
  var formattedDate = localDate.getFullYear() + "-" + (localDate.getMonth() + 1) + "-" + localDate.getDate();
  return formattedDate;
}

// 打印基本信息
function loadStatistical(sdata) {
  article_num = sdata.articleNum
  friends_num = sdata.friendsNum
  var messageBoard = `
  <div id="cf-state" class="cf-new-add">
    <div class="cf-state-data">
      <div class="cf-data-friends" onclick="openToShow()">
        <span class="cf-label">订阅</span>
        <span class="cf-message">${sdata.friendsNum}</span>
      </div>
      <div class="cf-data-active" onclick="changeEgg()">
        <span class="cf-label">活跃</span>
        <span class="cf-message">${sdata.activeNum}</span>
      </div>
      <div class="cf-data-article" onclick="clearLocal()">
        <span class="cf-label">日志</span>
        <span class="cf-message">${sdata.articleNum}</span>
      </div>
    </div>
  </div>
  `;
  var loadMoreBtn = `<div data-v-7eed2f8f><div data-v-7eed2f8f id="cf-more" class="cf-new-add" onclick="loadNextArticle()"><i class="fas fa-angle-double-down"></i></div></div>
    
  `;
  if(container){
    container.insertAdjacentHTML('beforebegin', messageBoard);
    container.insertAdjacentHTML('afterend', loadMoreBtn);
  }
}
// 打印文章内容 cf-article
function loadArticleItem(datalist, start, end) {
  var articleItem = "<div data-v-7eed2f8f>";
  var articleNum = article_num;
  var endFor = end
  if (end > articleNum) { endFor = articleNum }
  if (start < articleNum) {
    for (var i = start; i < endFor; i++) {
      var item = datalist[i].spec;
      articleItem += `
      <div data-v-7eed2f8f>
        <div data-v-7eed2f8f class="cf-article">
          <a class="cf-article-title" href="${item.link}" target="_blank" rel="noopener nofollow" data-title="${item.title}">${item.title}</a>
          <span class="cf-article-floor">${start + i + 1}</span>
          <div class="cf-article-avatar no-lightbox flink-item-icon">
            <img class="cf-img-avatar avatar" src="${item.logo}" alt="avatar" onerror="this.src='${fdata.error_img}'; this.onerror = null;">
            <a onclick="openMeShow(event)" data-link="${item.url}" class="" target="_blank" rel="noopener nofollow" href="javascript:;"><span class="cf-article-author">${item.author}</span></a>
            <span class="cf-article-time">
              <span class="cf-time-created"><i class="far fa-calendar-alt">发表于</i>${getDate(item.pubDate)}</span>
            </span>
          </div>
        </div>
      </div>
      `;
    }
    articleItem += "</div>"
    container.insertAdjacentHTML('beforeend', articleItem);
    // 预载下一页文章
    fetchNextArticle()
  } else {
    // 文章加载到底
    document.getElementById('cf-more').outerHTML = `<div id="cf-more" class="cf-new-add" onclick="loadNoArticle()"><small>一切皆有尽头！</small></div>`
  }
}
// 打印个人卡片 cf-overshow
function loadFcircleShow(articledata) {
  var userinfo = articledata[0].spec
  var showHtml = `
  <div >
    <div data-v-b8ec555f="" id="cf-overlay" onclick="closeShow()"></div>
    <div data-v-b8ec555f="" class="cf-overshow">
      <div data-v-b8ec555f="" class="cf-overshow-head"><img data-v-b8ec555f="" class="cf-img-avatar avatar"
              src="${userinfo.logo}" alt="avatar" onerror="this.src='${fdata.error_img}'; this.onerror = null;">
              <a data-v-b8ec555f=""
              class="" target="_blank" rel="noopener nofollow" href="${userinfo.url}">${userinfo.author}</a></div>
              <div data-v-b8ec555f="">
    `
  for (var i = 0; i < articledata.length; i++) {
    var item = articledata[i].spec;

    var contentClass = "cf-overshow-content"
    if(i+1==articledata.length){
      contentClass= "cf-overshow-content-tail";
    }
    showHtml += `
      <div data-v-b8ec555f="" class="${contentClass}">
          <p data-v-b8ec555f=""><a data-v-b8ec555f="" class="cf-article-title"
                  href="${item.link}" target="_blank" rel="noopener nofollow"
                  data-title="${item.title}">${item.title}</a><span
                  data-v-b8ec555f="">${getDate(item.pubDate)}</span></p>
      </div>
    `
  }
  showHtml += '</div></div></div>'
  document.getElementById('hexo-circle-of-friends-root').insertAdjacentHTML('beforebegin', showHtml);
}

// 预载下一页文章，存为本地数据 nextArticle
function fetchNextArticle() {
  var start = document.getElementsByClassName('cf-article').length
  var end = start + fdata.stepnumber
  var articleNum = article_num;
  if (end > articleNum) {
    end = articleNum
  }
  if (start < articleNum) {
    var localArticleData = JSON.parse(localStorage.getItem("ArticleData"));
    var nextArticle = eval(localArticleData.slice(start, end));
    localStorage.setItem("nextArticle", JSON.stringify(nextArticle))
  } else if (start = articleNum) {
    document.getElementById('cf-more').outerHTML = `<div id="cf-more" class="cf-new-add" onclick="loadNoArticle()"><small>一切皆有尽头！</small></div>`
  }
}
// 显示下一页文章，从本地缓存 nextArticle 中获取
function loadNextArticle() {
  var start = document.getElementsByClassName('cf-article').length
  var nextArticle = JSON.parse(localStorage.getItem("nextArticle"));
  var articleItem = ""
  for (var i = 0; i < nextArticle.length; i++) {
    var item = nextArticle[i].spec;

    articleItem += `
      <div data-v-7eed2f8f>
        <div data-v-7eed2f8f class="cf-article">
          <a class="cf-article-title" href="${item.link}" target="_blank" rel="noopener nofollow" data-title="${item.title}">${item.title}</a>
          <span class="cf-article-floor">${start + i + 1}</span>
          <div class="cf-article-avatar no-lightbox flink-item-icon">
            <img class="cf-img-avatar avatar" src="${item.logo}" alt="avatar" onerror="this.src='${fdata.error_img}'; this.onerror = null;">
            <a onclick="openMeShow(event)" data-link="${item.url}" class="" target="_blank" rel="noopener nofollow" href="javascript:;"><span class="cf-article-author">${item.author}</span></a>
            <span class="cf-article-time">
              <span class="cf-time-created"><i class="far fa-calendar-alt">发表于</i>${getDate(item.pubDate)}</span>
            </span>
          </div>
        </div>
      </div>
      `;
  }
  container.firstChild.insertAdjacentHTML('beforeend', articleItem);
  // 同时预载下一页文章
  fetchNextArticle()
}
// 没有更多文章
function loadNoArticle() {
  var articleSortData = "ArticleData"
  localStorage.removeItem(articleSortData)
  localStorage.removeItem("statisticalData")
  document.getElementById('cf-more').remove()
  window.scrollTo(0, document.getElementsByClassName('cf-state').offsetTop)
}
// 清空本地数据
function clearLocal() {
  localStorage.removeItem("updatedArticleData")
  localStorage.removeItem("createdArticleData")
  localStorage.removeItem("nextArticle")
  localStorage.removeItem("statisticalData")
  localStorage.removeItem("sortNow")
  localStorage.removeItem("urlNow")
  location.reload();
}
// 首次加载文章
function FetchFriendCircle() {
  var end = fdata.initnumber
  var fetchUrl = "/apis/api.plugin.halo.run/v1alpha1/plugins/plugin-friends/friendposts"
  fetch(fetchUrl)
    .then(res => res.json())
    .then(json => {
      var articleData = eval(json.items);
      fetch("/apis/api.plugin.halo.run/v1alpha1/plugins/plugin-friends/friend/statistical")
        .then(res => res.json())
        .then(json => {
          var statisticalData = json
          localStorage.setItem("statisticalData", JSON.stringify(statisticalData))
          localStorage.setItem("ArticleData", JSON.stringify(articleData))
          loadStatistical(statisticalData);
          loadArticleItem(articleData, 0, end)
        })

    })
}
//查询个人文章列表
function openMeShow(event) {
  event.preventDefault()
  var url = event.currentTarget.dataset.link
  var fetchUrl = `/apis/api.plugin.halo.run/v1alpha1/plugins/plugin-friends/friendPost/listByUrl?page=${1}&size=${5}&url=${url}`
  if (noClick == 'ok') {
    noClick = 'no'
    fetchShow(fetchUrl)
  }
}
// 关闭 show
function closeShow() {
  var cfOverlay = document.getElementById('cf-overlay');
  cfOverlay.parentNode.remove()
}
// 点击开往
var noClick = 'ok';
function openToShow() {
  var fetchUrl = ''
  if (fdata.apiurl) {
    fetchUrl = fdata.apiurl + "post"
  } else {
    fetchUrl = fdata.apipublieurl + "post"
  }
  if (noClick == 'ok') {
    noClick = 'no'
    fetchShow(fetchUrl)
  }
}
// 展示个人文章列表
function fetchShow(url) {
  fetch(url)
    .then(res => res.json())
    .then(json => {
      noClick = 'ok'
      var articleData = eval(json.items);
      loadFcircleShow(articleData)
    })
}
// 初始化方法，如有本地数据首先调用
function initFriendCircle() {
  container.innerHTML = "";
  FetchFriendCircle()
  // console.log("第一次加载完成")
}
// 执行初始化
initFriendCircle()
