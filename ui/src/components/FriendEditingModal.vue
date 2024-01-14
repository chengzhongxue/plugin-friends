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
  spec: {
    displayName: "",
    logo:"",
    rssUrl: "",
    link: "",
    description: "",
  },
  kind: "Friend",
  apiVersion: "friend.moony.la/v1alpha1",
};

const formState = ref<Friend>(cloneDeep(initialFormState));
const saving = ref<boolean>(false);
const formVisible = ref(false);


const isUpdateMode = computed(() => {
  return !!formState.value.metadata.creationTimestamp;
});

const modalTitle = computed(() => {
  return isUpdateMode.value ? "编辑订阅链接" : "新建订阅链接";
});

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
    if (isUpdateMode.value) {
      await apiClient.put<Friend>(
        `/apis/friend.moony.la/v1alpha1/friends/${formState.value.metadata.name}`,
        formState.value
      );
    } else {
      await apiClient.post<Friend>(
        `/apis/friend.moony.la/v1alpha1/friends`,
        formState.value
      );
    }

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
    :title="modalTitle"
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
            type="url"
            name="rssUrl"
            validation="required"
            label="订阅地址"
          ></FormKit>
          <FormKit type="attachment" name="logo" label="Logo"></FormKit>
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
