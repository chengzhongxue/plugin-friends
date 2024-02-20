var friends = {

    //从一个给定的数组arr中,随机返回num个不重复项
    getArrayItems: function (arr, num) {
        //新建一个数组,将传入的数组复制过来,用于运算,而不要直接操作传入的数组;
        var temp_array = new Array();
        for (var index in arr) {
            temp_array.push(arr[index]);
        }
        //取出的数值项,保存在此数组
        var return_array = new Array();
        for (var i = 0; i < num; i++) {
            //判断如果数组还有可以取出的元素,以防下标越界
            if (temp_array.length > 0) {
                //在数组中产生一个随机索引
                var arrIndex = Math.floor(Math.random() * temp_array.length);
                //将此随机索引的对应的数组元素值复制出来
                return_array[i] = temp_array[arrIndex];
                //然后删掉此索引的数组元素,这时候temp_array变为新的数组
                temp_array.splice(arrIndex, 1);
            } else {
                //数组中数据项取完后,退出循环,比如数组本来只有10项,但要求取出20项.
                break;
            }
        }
        return return_array;
    },

    addPopularBloggers: function () {
        var popularBloggers = document.getElementById("popular-bloggers");
        if (!popularBloggers) return;

        function getBlogs() {
            const fetchUrl = "/apis/api.plugin.halo.run/v1alpha1/plugins/plugin-friends/blogs"
            fetch(fetchUrl)
                    .then(res => res.json())
                    .then(json => {
                        saveToLocal.set('blogs-data', JSON.stringify(json), 10 / (60 * 24))
                        renderer(json);
                    })
        }

        function renderer(data) {
            const num = 15
            var randomBlog = friends.getArrayItems(data, num);
            var htmlText = '';
            for (let i = 0; i < randomBlog.length; ++i) {
                var item = randomBlog[i]
                    htmlText += `<div class="blogger-one">
                    <a href="/blogs/${item.metadata.name}"><img src="${item.spec.logo}"></a>
                    <span class="tooltiptext">${item.spec.displayName}</span>
                </div>`;
            }
            if (popularBloggers) {
                popularBloggers.innerHTML = htmlText;
            }
        }

        function friendBlogInit() {
            const data = saveToLocal.get('blogs-data')
            if (data) {
                renderer(JSON.parse(data))
            } else {
                getBlogs()
            }
        }

        friendBlogInit();
    },
}