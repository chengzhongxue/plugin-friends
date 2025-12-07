<script lang="ts" setup>
import {formatDatetime} from "@/utils/date";
import {type ListedRssSyncLog, RssFeedSyncLogStateEnum} from "@/api/generated";
import {
  VStatusDot,
  VEntity,
  VEntityField,
  VDropdownItem,
  VSpace,
  Dialog,
  Toast
} from "@halo-dev/components";
import {friendsApiClient} from "@/api";
import { useQueryClient } from "@tanstack/vue-query";
import {computed} from "vue";

const queryClient = useQueryClient();

const props = withDefaults(
  defineProps<{
    rssFeedSyncLog: ListedRssSyncLog;
  }>(),{

  }
);


const handleSyncRss = async () => {
  Dialog.warning({
    title: "同步RSS数据",
    description: "点击按钮后，后台将进行同步RSS数据",
    confirmText: "确定",
    cancelText: "取消",
    onConfirm: async () => {
      try {
        await friendsApiClient.friendPost.syncRssFeed({
          name: props.rssFeedSyncLog.link.metadata.name as string
        })

        Toast.success("同步成功");
      } catch (error) {
        console.error("Failed to synchronize rss", error);
      } finally {
        await queryClient.invalidateQueries({ queryKey: ["rssSyncLogs"] });
      }
    },
  });
};

const statusState = computed(() => {
  if (props.rssFeedSyncLog.log?.state == RssFeedSyncLogStateEnum.Success) {
     return "success";
  }
  if (props.rssFeedSyncLog.log?.state == RssFeedSyncLogStateEnum.Failed) {
    return "error";
  }
  return "warning";
});

const statusText = computed(() => {
  if (props.rssFeedSyncLog.log?.state == RssFeedSyncLogStateEnum.Success) {
    return "成功";
  }
  if (props.rssFeedSyncLog.log?.state == RssFeedSyncLogStateEnum.Failed) {
    return props.rssFeedSyncLog.log?.failureMessage;
  }
  if (props.rssFeedSyncLog.log?.state == RssFeedSyncLogStateEnum.Nolink) {
    return "无链接";
  }
  return "待同步";
});


</script>
<template>
  <VEntity >
    <template #start>
      <VEntityField>
        <template #title>
          <div class="entity-field-title">
            {{ rssFeedSyncLog.link.spec?.displayName }}
          </div>
        </template>
        <template #description>
          <div class=":uno: flex flex-col gap-1.5">
            <VSpace class=":uno: flex-wrap !gap-y-1">
              <span class=":uno: text-xs text-gray-500">
                {{ rssFeedSyncLog.link.metadata.annotations?.["rss_url"]}}
              </span>
            </VSpace>
          </div>
        </template>
      </VEntityField>
    </template>
    <template #end>
      <VEntityField >
        <template #description>
          <VStatusDot
            v-if="!rssFeedSyncLog.log"
            state="warning"
            animate
            :text="'待同步'"
          />
          <div
            v-else
            :class="`status-dot-wrapper status-dot-${statusState} status-dot-animate`"
          >
            <div class="status-dot-body"><span class="status-dot-inner"></span></div>
            <span class="status-dot-text" style="white-space: pre-wrap">{{statusText}}</span>
          </div>
        </template>
      </VEntityField>
      <VEntityField
        v-if="rssFeedSyncLog.log?.syncTime"
        v-tooltip="'同步时间'"
        :description="formatDatetime(rssFeedSyncLog.log?.syncTime)+' 同步'"
      ></VEntityField>
      <VEntityField v-if="rssFeedSyncLog.link.metadata.deletionTimestamp">
        <template #description>
          <VStatusDot
            v-tooltip="'删除中'"
            state="warning"
            animate
          />
        </template>
      </VEntityField>
    </template>
    <template #dropdownItems>
      <HasPermission :permissions="['plugin:forums:posts:manage']">
        <VDropdownItem type="danger" @click="handleSyncRss">
          重新获取
        </VDropdownItem>
      </HasPermission>
    </template>
  </VEntity>
</template>
