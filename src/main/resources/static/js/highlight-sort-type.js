var sortType = getQueryParams()['sort'];
var switchSortType = document.getElementsByClassName('switch-sort-type')
if(switchSortType.length>0){
    var menuItems = switchSortType[0].getElementsByClassName('menu')[0].getElementsByTagName('a');
    for (var i = 0; i < menuItems.length; i++) {
        var item = menuItems[i];
        if (undefined !== sortType && null !== sortType) {
            if (item.href.endsWith(sortType)) {
                item.classList.add("active");
            }
        } else {
            if (item.href.endsWith("access_count") || item.href.endsWith("recommended")) {
                item.classList.add("active");
            }
        }
    }
}

function getQueryParams() {
  const queryStr = window.location.search.substr(1);
  const params = {};
  queryStr.split('&').forEach(param => {
    const [key, value] = param.split('=');
    params[key] = decodeURIComponent(value);
  });
  return params;
}
console.log(getQueryParams());