<script lang="ts" setup>
import { Toast, VButton, VModal, VSpace } from "@halo-dev/components";
import { inject, ref, computed, nextTick, watch, type Ref } from "vue";
import type { Friend } from "@/types";
import apiClient from "@/api/request";
import cloneDeep from "lodash.clonedeep";

const props = withDefaults(
  defineProps<{
    visible: boolean;
    friend?: Friend;
  }>(),
  {
    visible: false,
    friend: undefined,
  }
);

const emit = defineEmits<{
  (event: "update:visible", value: boolean): void;
  (event: "close"): void;
}>();

const initialFormState: Friend = {
  metadata: {
    name: "",
    generateName: "friend-",
  },
  status:{
    statusType :'OK',
    code: 200
  },
  spec: {
    displayName: "",
    logo:"",
    rssUrl: "",
    link: "",
    description: "",
    submittedType: 'APPROVED'
  },
  kind: "Friend",
  apiVersion: "friend.moony.la/v1alpha1",
};

const formState = ref<Friend>(cloneDeep(initialFormState));
const saving = ref<boolean>(false);
const formVisible = ref(false);

const onVisibleChange = (visible: boolean) => {
  emit("update:visible", visible);
  if (!visible) {
    emit("close");
  }
};

const handleResetForm = () => {
  formState.value = cloneDeep(initialFormState);
};

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      formVisible.value = true;
    } else {
      setTimeout(() => {
        formVisible.value = false;
        handleResetForm();
      }, 200);
    }
  }
);

watch(
  () => props.friend,
  (friend) => {
    if (friend) {
      formState.value = cloneDeep(friend);
    }
  }
);

const annotationsFormRef = ref();

const handleSaveFriend = async () => {
  annotationsFormRef.value?.handleSubmit();
  await nextTick();

  const { customAnnotations, annotations, customFormInvalid, specFormInvalid } =
  annotationsFormRef.value || {};
  if (customFormInvalid || specFormInvalid) {
    return;
  }

  formState.value.metadata.annotations = {
    ...annotations,
    ...customAnnotations,
  };

  try {
    saving.value = true;
    await apiClient.post<Friend>(
      `/apis/api.plugin.halo.run/v1alpha1/plugins/plugin-friends/friend/Audit`,
      formState.value
    );
    Toast.success("保存成功");
    onVisibleChange(false);
  } catch (e) {
    console.error(e);
  } finally {
    saving.value = false;
  }
};
</script>
<template>
  <VModal
    title="审核订阅"
    :visible="visible"
    :width="650"
    @update:visible="onVisibleChange"
  >
    <template #actions>
      <slot name="append-actions" />
    </template>

    <FormKit
      v-if="formVisible"
      id="friend-form"
      v-model="formState.spec"
      name="friend-form"
      type="form"
      :config="{ validationVisibility: 'submit' }"
      @submit="handleSaveFriend"
    >
      <div class="md:grid md:grid-cols-4 md:gap-6">
        <div class="md:col-span-1">
          <div class="sticky top-0">
            <span class="text-base font-medium text-gray-900"> 常规 </span>
          </div>
        </div>
        <div class="mt-5 divide-y divide-gray-100 md:col-span-3 md:mt-0">
          <FormKit
            type="text"
            name="displayName"
            label="网站名称"
            disabled
          ></FormKit>
          <FormKit
            type="url"
            name="link"
            label="网站地址"
            disabled
          ></FormKit>
          <FormKit
            type="url"
            name="rssUrl"
            label="订阅地址"
            disabled
          ></FormKit>
        </div>
        <div class="md:col-span-1">
          <div class="sticky top-0">
            <span class="text-base font-medium text-gray-900"> 审核内容 </span>
          </div>
        </div>
        <div class="mt-5 divide-y divide-gray-100 md:col-span-3 md:mt-0">
          <FormKit
            :options="[
              { label: '提交', value: 'SUBMITTED' , attrs: { disabled: true } },
              { label: '系统检查有效', value: 'SYSTEM_CHECK_VALID' , attrs: { disabled: true } },
              { label: '系统检查无效', value: 'SYSTEM_CHECK_INVALID' , attrs: { disabled: true } },
              { label: '批准', value: 'APPROVED'},
              { label: '驳回', value: 'REJECTED'},
            ]"
            label="审核类型"
            name="submittedType"
            type="radio"
          ></FormKit>
          <FormKit
            type="text"
            name="reason"
            label="审核说明"
          ></FormKit>
        </div>
      </div>
    </FormKit>

    <template #footer>
      <VSpace>
        <VButton
          :loading="saving"
          type="secondary"
          @click="$formkit.submit('friend-form')"
        >
          提交
        </VButton>
        <VButton @click="onVisibleChange(false)">取消</VButton>
      </VSpace>
    </template>
  </VModal>
</template>
