<script lang="ts" setup>
import { 
  VCard,
  IconRefreshLine,
  Dialog,
  VButton,
  VEmpty,
  VLoading,
  VPagination,
  Toast,
  VSpace,
  IconCloseCircle} from "@halo-dev/components";
import {useQuery, useQueryClient} from "@tanstack/vue-query";
import {computed, ref, watch} from "vue";
import apiClient from "@/utils/api-client";
import { formatDatetime } from "@/utils/date";
import type {FriendPostList} from "@/types";
import {useRouteQuery} from "@vueuse/router";
import UserFilterDropdown from "@/components/filter/UserFilterDropdown.vue";

const queryClient = useQueryClient();

const selectedFriendPosts = ref<string[]>([]);
const checkedAll = ref(false);
const selectedSort = useRouteQuery<string | undefined>("sort");
const selectedAuthor = useRouteQuery<string | undefined>("author");

const page = ref(1);
const size = ref(20);
const keyword = ref("");
const searchText = ref("");
const total = ref(0);


watch(
  () => [
    selectedAuthor.value,
    selectedSort.value,
    keyword.value,
  ],
  () => {
    page.value = 1;
  }
);

function handleClearFilters() {
  selectedAuthor.value = undefined;
  selectedSort.value = undefined;
}

const hasFilters = computed(() => {
  return (
    selectedAuthor.value ||
    selectedSort.value 
  );
});

const {
  data: friendPosts,
  isLoading,
  isFetching,
  refetch,
} = useQuery({
  queryKey: ["friendPosts", page, size,selectedSort,selectedAuthor,keyword],
  queryFn: async () => {

    let authors: string | undefined;
    
    if (selectedAuthor.value) {
      authors = selectedAuthor.value;
    }
    const { data } = await apiClient.get<FriendPostList>(
      "/apis/api.plugin.halo.run/v1alpha1/plugins/plugin-friends/friendposts",
      {
        params: {
          page: page.value,
          size: size.value,
          sort: selectedSort.value,
          author : authors,
          keyword: keyword?.value
        },
      }
    );
    total.value = data.total;
    return data.items;
  },
  refetchInterval: (data) =>  {
    const deletingFriend = data?.filter(
      (friend) => !!friend.metadata.deletionTimestamp
    );
    return deletingFriend?.length ? 1000 : false;
  },
});


const handleCheckAllChange = (e: Event) => {
  const { checked } = e.target as HTMLInputElement;
  checkedAll.value = checked;
  if (checkedAll.value) {
    selectedFriendPosts.value =
      friendPosts.value?.map((friendPost) => {
        return friendPost.metadata.name;
      }) || [];
  } else {
    selectedFriendPosts.value.length = 0;
  }
};

const handleDeleteInBatch = () => {
  Dialog.warning({
    title: "是否确认删除所选的订阅文章？",
    description: "删除之后将无法恢复。",
    confirmType: "danger",
    onConfirm: async () => {
      try {
        const promises = selectedFriendPosts.value.map((friendPost) => {
          return apiClient.delete(`/apis/friend.moony.la/v1alpha1/friendposts/${friendPost}`);
        });
        if (promises) {
          await Promise.all(promises);
        }
        selectedFriendPosts.value.length = 0;
        checkedAll.value = false;

        Toast.success("删除成功");
      } catch (e) {
        console.error(e);
      } finally {
        queryClient.invalidateQueries({ queryKey: ["friendPosts"] });
      }
    },
  });
};
function handleReset() {
  keyword.value = "";
  searchText.value = "";
}
function onKeywordChange() {
  keyword.value = searchText.value;
}

</script>

<template>

  <VCard :body-class="['!p-0']">
    <template #header>
      <div class="block w-full bg-gray-50 px-4 py-3">
        <div class="relative flex flex-col flex-wrap items-start gap-4 sm:flex-row sm:items-center" >
          <div class="hidden items-center sm:flex" >
            <input
              v-model="checkedAll"
              type="checkbox"
              @change="handleCheckAllChange"
            />
          </div>
          <div class="flex w-full flex-1 items-center sm:w-auto" >
            <FormKit
              v-if="!selectedFriendPosts.length"
              v-model="searchText"
              placeholder="输入关键词搜索"
              type="text"
              outer-class="!moments-p-0 moments-mr-2"
              @keyup.enter="onKeywordChange"
            >
              <template v-if="keyword" #suffix>
                <div
                  class="group flex h-full cursor-pointer items-center bg-white px-2 transition-all hover:bg-gray-50"
                  @click="handleReset"
                >
                  <IconCloseCircle
                    class="h-4 w-4 text-gray-500 group-hover:text-gray-700"
                  />
                </div>
              </template>
            </FormKit>
            <VSpace v-else v-permission="['plugin:friends:manage']">
              <VButton type="danger" @click="handleDeleteInBatch">
                删除
              </VButton>
            </VSpace>
          </div>
          <VSpace spacing="lg" class="flex-wrap">
              <FilterCleanButton
                v-if="hasFilters"
                @click="handleClearFilters"
              />
              <UserFilterDropdown
                v-model="selectedAuthor"
                label="作者"
              />
              <FilterDropdown
                v-model="selectedSort"
                label="排序"
                :items="[
                      {
                        label: '默认',
                      },
                      {
                        label: '较近创建',
                        value: 'pubDate,desc',
                      },
                      {
                        label: '较早创建',
                        value: 'pubDate,asc',
                      },
                    ]"
              />
              <div class="flex flex-row gap-2">
                <div
                  class="group cursor-pointer rounded p-1 hover:bg-gray-200"
                  @click="refetch()"
                >
                  <IconRefreshLine
                    v-tooltip="'刷新'"
                    :class="{ 'animate-spin text-gray-900': isFetching }"
                    class="h-4 w-4 text-gray-600 group-hover:text-gray-900"
                  />
                </div>
              </div>
          </VSpace>
        </div>
      </div>
    </template>
    <VLoading v-if="isLoading" />

    <Transition v-else-if="!friendPosts?.length" appear name="fade">
      <VEmpty
        message="暂无订阅文章记录"
        title="暂无订阅文章记录"
      >
        <template #actions>
          <VSpace>
            <VButton @click="refetch()"> 刷新 </VButton>
          </VSpace>
        </template>
      </VEmpty>
    </Transition>

    <Transition v-else appear name="fade">
      <div class="w-full relative overflow-x-auto">
        <table class="w-full text-sm text-left text-gray-500">
          <thead class="text-xs text-gray-700 uppercase bg-gray-50">
             <tr>
               <th scope="col" class="px-4 py-3"><div class="w-max flex items-center"> </div></th>
               <th scope="col" class="px-4 py-3"><div class="w-max flex items-center">作者名称 </div></th>
               <th scope="col" class="px-4 py-3"><div class="w-max flex items-center">站点链接 </div></th>
               <th scope="col" class="px-4 py-3"><div class="w-max flex items-center">文章标题 </div></th>
               <th scope="col" class="px-4 py-3"><div class="w-max flex items-center">文章链接 </div></th>
               <th scope="col" class="px-4 py-3"><div class="w-max flex items-center">文章内容 </div></th>
               <th scope="col" class="px-4 py-3"><div class="w-max flex items-center">发布时间 </div></th>
             </tr>
          </thead>
          <tbody>
             <tr v-for="friendPost in friendPosts" class="border-b last:border-none hover:bg-gray-100">
               <td class="px-4 py-4 ">
                 <input
                   v-model="selectedFriendPosts"
                   :value="friendPost.metadata.name"
                   class="h-4 w-4 rounded border-gray-300 text-indigo-600"
                   name="post-checkbox"
                   type="checkbox"
                 />
               </td>
               <td  class="px-4 py-4 table-td">{{friendPost.spec.author}}</td>
               <td class="px-4 py-4 table-td">
                 <a
                   :href="friendPost.spec.url"
                   class="hover:text-gray-900"
                   target="_blank"
                 >
                   {{ friendPost.spec.url }}
                 </a>
               </td>
               <td class="px-4 py-4">{{friendPost.spec.title}}</td>
               <td class="px-4 py-4 ant-link">
                 <a
                   :href="friendPost.spec.link"
                   class="hover:text-gray-900"
                   target="_blank"
                 >
                   {{friendPost.spec.link}}
                 </a>
               </td>
               <td class="px-4 py-4 ant-td">{{friendPost.spec.description}}</td>
               <td class="px-4 py-4 table-td">{{formatDatetime(friendPost.spec.pubDate)}}</td>
             </tr>
          </tbody>
        </table>
      </div>
    </Transition>

    <template #footer>
      <VPagination
        v-model:page="page"
        v-model:size="size"
        :total="total"
        :size-options="[20, 30, 50, 100]"
      />
    </template>
  </VCard>
  
</template>

<style scoped lang="scss">

.table-td {
  text-align: left !important;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ant-link {
  max-width: 250px;
  border-bottom: 0;
  text-align: left !important;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ant-td {
  max-width: 410px;
  border-bottom: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  -webkit-line-clamp: 3;
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  padding-right: 1rem;
  padding-bottom: 0rem;
  margin-bottom: 1.1em;
}


</style>
