<script setup lang="ts">
import {markRaw, shallowRef} from "vue";
import FriendPostTab from "@/views/FriendPostTab.vue";
import CronTab from "@/views/CronTab.vue";
import { useRouteQuery } from "@vueuse/router";
import { Dialog, Toast } from "@halo-dev/components";

import {
  VButton,
  VCard,
  VPageHeader,
  VSpace,
  VTabbar,
} from "@halo-dev/components";
import RssFeedSyncLogTab from "@/views/RssFeedSyncLogTab.vue";
import {friendsApiClient} from "@/api";
import {axiosInstance} from "@halo-dev/api-client";

const tabs = shallowRef([
  {
    id: "friendPost",
    label: "订阅文章",
    component: markRaw(FriendPostTab),
  },
  {
    id: "cron",
    label: "定时任务",
    component: markRaw(CronTab),
  },
  {
    id: "rssFeedSyncLog",
    label: "同步日志",
    component: markRaw(RssFeedSyncLogTab),
  }

]);

const activeIndex = useRouteQuery<string>("tab", tabs.value[0].id);

//新增导出
const handleCreate = () => {
  Dialog.warning({
    title: "同步RSS数据",
    description: "点击按钮后，后台将进行同步RSS数据。",
    confirmType: "danger",
    confirmText: "确定",
    cancelText: "取消",
    onConfirm: async () => {
      try {
        await friendsApiClient.friendPost.syncRssFeed({
          name: "all"
        })
        Toast.success("同步RSS数据成功");
      } catch (e) {
        console.error("", e);
      }
    },
  });
}

const handleDel = () => {
  Dialog.warning({
    title: "清空RSS数据",
    description: "点击按钮后，将进行删除RSS数据。",
    confirmType: "danger",
    confirmText: "确定",
    cancelText: "取消",
    onConfirm: async () => {
      try {
        await axiosInstance.delete("/apis/api.friend.moony.la/v1alpha1/friendposts/-/delete")
        Toast.success("清空RSS数据成功");
      } catch (e) {
        console.error("", e);
      }
    },
  });
}

</script>

<template>

  <VPageHeader title="RSS订阅">
    <template #actions>
      <VSpace v-permission="['plugin:friends:manage']">
        <VButton type="secondary" @click="handleCreate">
          同步数据
        </VButton>
        <VButton type="danger" @click="handleDel">
          清空
        </VButton>
      </VSpace>
    </template>

  </VPageHeader>

  <div class=":uno: m-0 md:m-4">
    <VCard :body-class="[':uno: !p-0']">
      <template #header>
        <VTabbar
          v-model:active-id="activeIndex"
          :items="tabs.map((item) => ({ id: item.id, label: item.label }))"
          class=":uno: w-full !rounded-none"
          type="outline"
        ></VTabbar>
      </template>
      <div class=":uno: bg-white">
        <FriendPostTab ref="friendPost" v-if="activeIndex=='friendPost'"/>
        <CronTab ref="cron" v-if="activeIndex=='cron'"/>
        <RssFeedSyncLogTab ref="rssFeedSyncLog" v-if="activeIndex=='rssFeedSyncLog'"/>
      </div>
    </VCard>
  </div>


</template>
