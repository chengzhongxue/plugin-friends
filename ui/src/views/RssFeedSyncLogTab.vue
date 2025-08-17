<script lang="ts" setup>
import {ref, watch} from "vue";
import {
  VButton,
  VCard,
  VEmpty,
  VSpace,
  VLoading,
  VPagination,
  IconRefreshLine,
  VEntityContainer
} from "@halo-dev/components";
import { useRouteQuery } from "@vueuse/router";
import {friendsApiClient} from "@/api";
import { useQuery } from "@tanstack/vue-query";
import RssFeedSyncLogListItem from "@/components/RssFeedSyncLogListItem.vue";


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
    keyword.value,
  ],
  () => {
    page.value = 1;
  },
);

const {
  data: rssSyncLogs,
  isLoading,
  isFetching,
  refetch,
} = useQuery({
  queryKey: [
    "rssSyncLogs",
    page,
    size,
    keyword,
  ],
  queryFn: async () => {

    const { data } = await friendsApiClient.friendPost.listRssSyncLogs({
      page: page.value,
      size: size.value,
      keyword: keyword.value
    });

    total.value = data.total;
    return data.items;
  },
  refetchInterval: (data) => {
    const hasDeletingRssSyncLog = data?.some(
      (rssSyncLog) => rssSyncLog?.link.metadata.deletionTimestamp,
    );
    return hasDeletingRssSyncLog ? 1000 : false;
  },
});

</script>
<template>
  <VCard :body-class="[':uno: !p-0']">
    <template #header>
      <div class=":uno: block w-full bg-gray-50 px-4 py-3">
        <div
          class=":uno: relative flex h-9 flex-col flex-wrap items-start gap-4 sm:flex-row sm:items-center"
        >
          <div class=":uno: flex w-full flex-1 items-center sm:w-auto">
            <SearchInput
              v-model="keyword"
            />
          </div>
          <VSpace spacing="lg" class=":uno: flex-wrap">
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
    <Transition v-else-if="!rssSyncLogs?.length" appear name="fade">
      <VEmpty
        :title="'当前没有日志'"
      >
        <template #actions>
          <VSpace>
            <VButton @click="refetch">
              刷新
            </VButton>
          </VSpace>
        </template>
      </VEmpty>
    </Transition>

    <Transition appear name="fade">
      <VEntityContainer>
        <RssFeedSyncLogListItem
          v-for="(rssSyncLog, index) in rssSyncLogs" :key="index"
          :rss-feed-sync-log="rssSyncLog"
        >
        </RssFeedSyncLogListItem>
      </VEntityContainer>
    </Transition>
    <template #footer>
      <VPagination
        v-model:page="page"
        v-model:size="size"
        page-label="页"
        size-label="条 / 页"
        :total-label="`共 ${total} 项数据`"
        :total="total"
        :size-options="[30, 60, 90]"
      />
    </template>
  </VCard>
</template>
