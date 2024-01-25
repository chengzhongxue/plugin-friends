<script lang="ts" setup>
import {useAuthorFetch} from "@/api/use-author";
import type  {Friend} from "@/types";
import {
  IconArrowDown,
  VAvatar,
  VDropdown,
  VEntity,
  VEntityField,
} from "@halo-dev/components";
import { computed, ref, watch } from "vue";
import Fuse from "fuse.js";

import {setFocus} from "@/utils/focus";

const props = withDefaults(
  defineProps<{
    label: string;
    modelValue?: string;
  }>(),
  {
    modelValue: undefined,
  }
);

const emit = defineEmits<{
  (event: "update:modelValue", value?: string): void;
}>();

const { authors } = useAuthorFetch({ fetchOnMounted: true });

const dropdown = ref();

const handleSelect = (author: Friend) => {
  if (author.spec.displayName === props.modelValue) {
    emit("update:modelValue", undefined);
  } else {
    emit("update:modelValue", author.spec.displayName);
  }

  dropdown.value.hide();
};

function onDropdownShow() {
  setTimeout(() => {
    setFocus("userFilterDropdownInput");
  }, 200);
}

// search
const keyword = ref("");

let fuse: Fuse<Friend> | undefined = undefined;

watch(
  () => authors.value,
  () => {
    fuse = new Fuse(authors.value, {
      keys: ["spec.displayName", "metadata.name", "spec.adminEmail"],
      useExtendedSearch: true,
      threshold: 0.2,
    });
  }
);

const searchResults = computed(() => {
  if (!fuse || !keyword.value) {
    return authors.value;
  }

  return fuse?.search(keyword.value).map((item: { item: any; }) => item.item);
});

const selectedUser = computed(() => {
  return authors.value.find((author) => author.spec.displayName === props.modelValue);
});
</script>

<template>
  <VDropdown ref="dropdown" :classes="['!p-0']" @show="onDropdownShow">
    <div
      class="flex cursor-pointer select-none items-center text-sm text-gray-700 hover:text-black"
      :class="{ 'font-semibold text-gray-700': modelValue !== undefined }"
    >
      <span v-if="!selectedUser" class="mr-0.5">
        {{ label }}
      </span>
      <span v-else class="mr-0.5">
        {{ label }}：{{ selectedUser.spec.displayName }}
      </span>
      <span>
        <IconArrowDown />
      </span>
    </div>
    <template #popper>
      <div class="h-96 w-80">
        <div class="border-b border-b-gray-100 bg-white p-4">
          <FormKit
            id="userFilterDropdownInput"
            v-model="keyword"
            placeholder="输入关键词搜索"
            type="text"
          ></FormKit>
        </div>
        <div>
          <ul
            class="box-border h-full w-full divide-y divide-gray-100"
            role="list"
          >
            <li
              v-for="(author, index) in searchResults"
              :key="index"
              @click="handleSelect(author)"
            >
              <VEntity :is-selected="modelValue === author.spec.displayName">
                <template #start>
                  <VEntityField>
                    <template #description>
                      <VAvatar
                        :key="author.spec.link"
                        :alt="author.spec.displayName"
                        :src="author.spec.logo"
                        size="md"
                      ></VAvatar>
                    </template>
                  </VEntityField>
                  <VEntityField
                    :title="author.spec.displayName"
                    :description="author.spec.link"
                  />
                </template>
              </VEntity>
            </li>
          </ul>
        </div>
      </div>
    </template>
  </VDropdown>
</template>
