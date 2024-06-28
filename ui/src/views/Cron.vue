<script lang="ts" setup>
import {VButton} from "@halo-dev/components";
import {computed, onMounted, ref} from "vue";
import type {CronFriendPost, CronFriendPostList} from "@/types";
import cloneDeep from "lodash.clonedeep";
import apiClient from "@/utils/api-client";

const Se = "cron-friends-default"

const initialFormState: CronFriendPost = {
  metadata: {
    name: Se,
    creationTimestamp: ""
  },
  spec: {
    cron: "0 0 * * * *",
    timezone:"Asia/Shanghai",
    suspend: false,
    autoverify: true,
    successfulRetainLimit: 0,
  },
  kind: "CronFriendPost",
  apiVersion: "friend.moony.la/v1alpha1",
};


const isUpdateMode = computed(() => {
  return !!formState.value.metadata.creationTimestamp;
});

const  saving = ref(false);
const formState = ref<CronFriendPost>(cloneDeep(initialFormState));
const formSchema = ref(
  [

    {
      $formkit: 'checkbox',
      name: 'suspend',
      label: '是否启用',
      value: false,
      help: '定时获取RSS订阅数据'
    },
    {
      $formkit: 'checkbox',
      name: 'autoverify',
      label: '是否启用自动审核提交的博客',
      value: true,
      help: '定时审核自动提交的订阅博客'
    },
    {
      $cmp: 'FormKit',
      props: {
        type: 'text',
        name: 'cron',
        label: '定时表达式',
        validation: 'required',
        help: '定时任务表达式，请参考：https://moony.la/cron'
      }
    },
    {
      $cmp: 'FormKit',
      props: {
        type: 'select',
        name: 'timezone',
        label: '时区',
        options: [
          {value: "Asia/Shanghai", label: 'Asia/Shanghai (GMT+08:00)'},
        ],
      }
    },
    {
      $formkit: 'number',
      name: 'successfulRetainLimit',
      label: '成功保留限制条数',
      help: '设置之后会保留的数据条数，设置为 0 即为5条',
      number: "integer",
      validation: 'required|number|min:0',
    },
  ]
)

const mutate = async () => {
  saving.value = true;
  try {
    if (isUpdateMode.value) {
      const {
        data: data
      } = await apiClient.get(`/apis/friend.moony.la/v1alpha1/cronfriendposts/${Se}`);
      return formState.value = {
        ...formState.value,
        status: data.status,
        metadata: data.metadata
      },
      await apiClient.put<CronFriendPost>(
        `/apis/friend.moony.la/v1alpha1/cronfriendposts/${Se}`,
        formState.value
      );
    } else {
      await apiClient.post<CronFriendPost>(
        `/apis/friend.moony.la/v1alpha1/cronfriendposts`,
        formState.value
      );
    }
  } finally {
    saving.value = false;
  }
}

onMounted(async () => {

  const {data: data} = await apiClient.get<CronFriendPostList>(`/apis/friend.moony.la/v1alpha1/cronfriendposts`);
  let items = data.items;
  if (items?.length){
    formState.value = items[0]
  }
  
});

</script>

<template>
  <Transition mode="out-in" name="fade">
    <div class="bg-white p-4">
      <div>
        <FormKit
          id="cron-setting"
          v-model="formState.spec"
          name="cron-setting"
          :actions="false"
          :preserve="true"
          type="form"
          @submit="mutate"
          submit-label="Login"
        >
          <FormKitSchema :schema="formSchema"/>
        </FormKit>
      </div>
      <div v-permission="['plugin:friends:manage']" class="pt-5">
        <div class="flex justify-start">
          <VButton
            :loading="saving"
            type="secondary"
            @click="$formkit.submit('cron-setting')"
          >
            保存
          </VButton>
        </div>
      </div>
    </div>
  </Transition>
</template>
