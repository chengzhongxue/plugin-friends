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
} from "@halo-dev/components";
import {useQuery, useQueryClient} from "@tanstack/vue-query";
import {computed, ref, watch} from "vue";
import {useRouteQuery} from "@vueuse/router";
import LinkFilterDropdown from "@/components/filter/LinkFilterDropdown.vue";
import {friendsApiClient, friendsCoreApiClient} from "@/api";
import { utils } from '@halo-dev/ui-shared'

const queryClient = useQueryClient();

const selectedSort = useRouteQuery<string | undefined>("sort");
const selectedLink = useRouteQuery<string | undefined>("author");

const keyword = useRouteQuery<string>("keyword", "");
const page = useRouteQuery<number>("page", 1, {
  transform: Number,
});
const size = useRouteQuery<number>("size", 30, {
  transform: Number,
});
const total = ref(0);


watch(
  () => [
    selectedLink.value,
    selectedSort.value,
    keyword.value,
  ],
  () => {
    page.value = 1;
  }
);

function handleClearFilters() {
  selectedLink.value = undefined;
  selectedSort.value = undefined;
}

const hasFilters = computed(() => {
  return (
    selectedLink.value ||
    selectedSort.value
  );
});

const {
  data: friendPosts,
  isLoading,
  isFetching,
  refetch,
} = useQuery({
  queryKey: ["friendPosts", page, size,selectedSort,selectedLink,keyword],
  queryFn: async () => {

    let linkName: string | undefined;

    if (selectedLink.value) {
      linkName = selectedLink.value;
    }
    const { data } = await friendsApiClient.friendPost.listFriendPosts(
      {
        page: page.value,
        size: size.value,
        sort: [selectedSort.value].filter(Boolean) as string[],
        linkName : linkName,
        keyword: keyword?.value
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


const handleDelete = async (name: string) => {
  Dialog.warning({
    title: "确定要删除该应用吗？",
    description: "删除之后将无法恢复。",
    confirmType: "danger",
    confirmText: "确定",
    cancelText: "取消",
    onConfirm: async () => {
      try {
        await friendsCoreApiClient.friendPost.deleteFriendPost({
          name: name
        })

        Toast.success("删除成功");
      } catch (e) {
        console.error(e);
      } finally {
        queryClient.invalidateQueries({ queryKey: ["friendPosts"] });
      }
    },
  });
};

</script>

<template>

  <VCard :body-class="[':uno: !p-0']" >
    <template #header>
      <div class=":uno: block w-full bg-gray-50 px-4 py-3">
        <div class=":uno: relative flex flex-col flex-wrap items-start gap-4 sm:flex-row sm:items-center" >
          <div class=":uno: flex w-full flex-1 items-center sm:w-auto">
            <SearchInput
              v-model="keyword"
            />
          </div>
          <VSpace spacing="lg" class=":uno: flex-wrap">
              <FilterCleanButton
                v-if="hasFilters"
                @click="handleClearFilters"
              />
              <LinkFilterDropdown
                v-model="selectedLink"
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
                      value: 'spec.pubDate,desc',
                    },
                    {
                      label: '较早创建',
                      value: 'spec.pubDate,asc',
                    },
                  ]"
              />
              <div class=":uno: flex flex-row gap-2">
                <div
                  class=":uno: group cursor-pointer rounded p-1 hover:bg-gray-200"
                  @click="refetch()"
                >
                  <IconRefreshLine
                    v-tooltip="'刷新'"
                    :class="{ ':uno: animate-spin text-gray-900': isFetching }"
                    class=":uno: h-4 w-4 text-gray-600 group-hover:text-gray-900"
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
      <div class=":uno: m-0 md:m-4">
        <ul class="thyuu-card">
          <li v-for="friendPost in friendPosts">
            <header>
              <img alt="" :src="friendPost.spec.logo"
                   class=":uno: avatar avatar-24 photo" height="24" width="24" loading="lazy" decoding="async">
              <span>{{friendPost.spec.author}}</span>
              <span></span>
            </header>
            <article>
              <h4>
                <a :href="friendPost.spec.postLink" target="_blank"
                   rel="noopener noreferrer">
                  {{friendPost.spec.title}}
                </a>
              </h4>
              <p>{{friendPost.spec.description}}</p>
            </article>
            <footer>
              <time>{{utils.date.format(friendPost.spec.pubDate)}} 发布</time>
              <a :href="friendPost.spec.postLink" class=":uno: button icon-views" target="_blank"
                 rel="noopener noreferrer">访问动态</a>
              <HasPermission :permissions="['plugin:friends:manage']">
                <a @click="handleDelete(friendPost.metadata.name)"
                   class=":uno: button icon-del im cursor-pointer">删除</a>
              </HasPermission>
            </footer>
          </li>
        </ul>
      </div>
    </Transition>

    <template #footer>
      <VPagination
        v-model:page="page"
        v-model:size="size"
        :total="total"
        :size-options="[30, 60, 90]"
      />
    </template>
  </VCard>

</template>
