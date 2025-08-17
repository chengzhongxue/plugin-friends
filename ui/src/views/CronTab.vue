<script lang="ts" setup>
import {Toast, VButton} from "@halo-dev/components";
import {ref} from "vue";
import type {CronFriendPost} from "@/api/generated";
import cloneDeep from "lodash.clonedeep";
import {friendsCoreApiClient} from "@/api";
import LinkFormKit from "@/components/formkit/LinkFormKit.vue";
import {useMutation, useQuery} from "@tanstack/vue-query";

const Se = "cron-friends-default"

const initialFormState: CronFriendPost = {
  metadata: {
    name: Se,
    creationTimestamp: ""
  },
  spec: {
    cron: "@daily",
    timezone:"Asia/Shanghai",
    suspend: false,
    successfulRetainLimit: 0,
    disableSyncList: undefined,
  },
  kind: "CronFriendPost",
  apiVersion: "friend.moony.la/v1alpha1",
};

const cronOptions = [{
  label: "每月（每月 1 号 0 点）",
  value: "@monthly"
}, {
  label: "每周（每周第一天 的 0 点）",
  value: "@weekly"
}, {
  label: "每天（每天的 0 点）",
  value: "@daily"
}, {
  label: "每小时",
  value: "@hourly"
}]

const formState = ref<CronFriendPost>(cloneDeep(initialFormState));

const {isLoading: cronIsLoading, isFetching: cronIsFetching} = useQuery({
  queryKey: ["cron-friend-post"],
  queryFn: async () => {
    const {data} = await friendsCoreApiClient.cron.getCronFriendPost({
      name: Se
    },{
      mute: true
    });
    return data;
  },
  onSuccess(data) {
    formState.value =  data
  },
  retry: false
})

const { mutate:save, isLoading:saveIsLoading } = useMutation({
  mutationKey: ["cron-friend-post-save"],
  mutationFn: async () => {
    if (formState.value.metadata.creationTimestamp) {
      const { data: data } = await friendsCoreApiClient.cron.getCronFriendPost({
        name: Se
      });
      formState.value = {
        ...formState.value,
        status: data.status,
        metadata: data.metadata
      };
      return await friendsCoreApiClient.cron.updateCronFriendPost({
          name: Se,
          cronFriendPost: formState.value
        });
    }else {
      return await friendsCoreApiClient.cron.createCronFriendPost({
        cronFriendPost: formState.value
      });
    }
  },
  onSuccess(data) {
    formState.value = data.data
    Toast.success("保存成功");
  }
});

</script>



<template>
  <Transition mode="out-in" name="fade">
    <div class=":uno: bg-white p-4">
      <div>
        <FormKit
          id="cron-setting-form"
          v-model="formState.spec"
          name="cron-setting-form"
          :preserve="true"
          type="form"
          :disabled="cronIsFetching"
          @submit="save"
        >
          <FormKit
            type="checkbox"
            name="suspend"
            label="是否启用"
            value="false"
            help="定时获取RSS订阅数据"
          />
          <FormKit
            type="select"
            name="cron"
            label="定时表达式"
            allow-create
            searchable
            validatio="required"
            :options="cronOptions"
            help="定时表达式规则请参考：https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-cron-expression"
          />
          <FormKit
            type="select"
            name="timezone"
            label="时区"
            :options="[
               {
                 value: 'Asia/Shanghai',
                 label: 'Asia/Shanghai (GMT+08:00)'
               },
            ]"
          />
          <FormKit
            type="number"
            name="successfulRetainLimit"
            label="留限制条数"
            number="integer"
            validation="required|number|min:0"
            help="设置之后会保留的数据条数，设置为 0 即为5条"
          />
          <LinkFormKit
            name="disableSyncList"
            label="禁止同步名单"
          ></LinkFormKit>
        </FormKit>
      </div>
      <div v-permission="['plugin:friends:manage']" class=":uno: pt-5">
        <div class=":uno: flex justify-start">
          <VButton
            :loading="saveIsLoading"
            :disabled="cronIsLoading"
            type="secondary"
            @click="$formkit.submit('cron-setting-form')"
          >
            保存
          </VButton>
        </div>
      </div>
    </div>
  </Transition>
</template>
