<script lang="ts" setup>
import { VCard, VEntity, IconList, IconArrowLeft,IconArrowRight, VEntityField,Dialog,VButton,VEmpty,VLoading,VPagination,Toast, VDropdownItem,  IconAddCircle, VSpace} from "@halo-dev/components";
import { ref} from "vue";
import { useFriendFetch} from "@/api/use-friend";
import type { Friend } from "@/types";
import service from "@/api/request";
import { useQueryClient } from "@tanstack/vue-query";
import { formatDatetime } from "@/utils/date";
import FriendEditingModal from "../components/FriendEditingModal.vue";

const queryClient = useQueryClient();

const selectedFriend = ref<Friend | undefined>();
const selectedFriends = ref<string[]>([]);
const checkedAll = ref(false);
const editingModal = ref(false);

const page = ref(1);
const size = ref(20);
const keyword = ref("");
const searchText = ref("");

const { friends, isLoading, total, refetch } = useFriendFetch(
  page,
  size,
  keyword
);

const handleCheckAllChange = (e: Event) => {
  const { checked } = e.target as HTMLInputElement;
  checkedAll.value = checked;
  if (checkedAll.value) {
    selectedFriends.value =
      friends.value?.map((friend) => {
        return friend.metadata.name;
      }) || [];
  } else {
    selectedFriends.value.length = 0;
  }
};

const handleDelete = (friend: Friend) => {
  Dialog.warning({
    title: "是否确认删除当前的订阅链接？",
    description: "删除之后将无法恢复。",
    confirmType: "danger",
    onConfirm: async () => {
      try {
        await service.delete(
          `/apis/friend.moony.la/v1alpha1/friends/${friend.metadata.name}`
        );

        Toast.success("删除成功");
      } catch (e) {
        console.error(e);
      } finally {
        queryClient.invalidateQueries({ queryKey: ["friends"] });
      }
    },
  });
};

const handleDeleteInBatch = () => {
  Dialog.warning({
    title: "是否确认删除所选的链接？",
    description: "删除之后将无法恢复。",
    confirmType: "danger",
    onConfirm: async () => {
      try {
        const promises = selectedFriends.value.map((friend) => {
          return service.delete(`/apis/friend.moony.la/v1alpha1/friends/${friend}`);
        });
        if (promises) {
          await Promise.all(promises);
        }

        selectedFriends.value.length = 0;
        checkedAll.value = false;

        Toast.success("删除成功");
      } catch (e) {
        console.error(e);
      } finally {
        queryClient.invalidateQueries({ queryKey: ["friends"] });
      }
    },
  });
};


const onEditingModalClose = async () => {
  selectedFriend.value = undefined;
  refetch();
};

const handleSelectPrevious = () => {
  if (!friends.value) {
    return;
  }

  const currentIndex = friends.value.findIndex(
    (friend) => friend.metadata.name === selectedFriend.value?.metadata.name
  );

  if (currentIndex > 0) {
    selectedFriend.value = friends.value[currentIndex - 1];
    return;
  }

  if (currentIndex <= 0) {
    selectedFriend.value = undefined;
  }
};

const handleSelectNext = () => {
  if (!friends.value) return;

  if (!selectedFriend.value) {
    selectedFriend.value = friends.value[0];
    return;
  }
  const currentIndex = friends.value.findIndex(
    (friend) => friend.metadata.name === selectedFriend.value?.metadata.name
  );
  if (currentIndex !== friends.value.length - 1) {
    selectedFriend.value = friends.value[currentIndex + 1];
  }
};

const handleOpenCreateModal = (friend: Friend) => {
  selectedFriend.value = friend;
  editingModal.value = true;
};

</script>

<template>

  <FriendEditingModal
    v-model:visible="editingModal"
    :friend="selectedFriend"
    @close="onEditingModalClose"
  >
    <template #append-actions>
      <span @click="handleSelectPrevious">
        <IconArrowLeft />
      </span>
      <span @click="handleSelectNext">
        <IconArrowRight />
      </span>
    </template>
  </FriendEditingModal>
  <VCard :body-class="['!p-0']">
    <template #header>
      <div
        class="block w-full bg-gray-50 px-4 py-3"
      >
        <div
          class="relative flex flex-col items-start sm:flex-row sm:items-center"
        >
          <div
            class="mr-4 hidden items-center sm:flex"
          >
            <input
              v-model="checkedAll"
              class="h-4 w-4 rounded border-gray-300 text-indigo-600"
              type="checkbox"
              @change="handleCheckAllChange"
            />
          </div>
          <div
            class="flex w-full flex-1 items-center sm:w-auto"
          >
            <div class="flex items-center gap-2" >
              <FormKit
                v-if="!selectedFriends.length"
                v-model="searchText"
                outer-class="!p-0"
                placeholder="输入关键词搜索"
                type="text"
                @keyup.enter="keyword = searchText"
              ></FormKit>
            </div>
            <VSpace v-if="selectedFriends.length">
              <VButton type="danger" @click="handleDeleteInBatch">
                删除
              </VButton>
            </VSpace>
          </div>
          <div
            v-permission="['plugin:friends:manage']"
            class="mt-4 flex sm:mt-0"
          >
            <VButton size="xs" @click="editingModal = true">
              新建
            </VButton>
          </div>
        </div>
      </div>
    </template>
    <VLoading v-if="isLoading" />
    <Transition v-else-if="!friends?.length" appear name="fade">
      <VEmpty message="你可以尝试刷新或者新建订阅链接" title="当前没有订阅链接">
        <template #actions>
          <VSpace>
            <VButton @click="refetch"> 刷新</VButton>
            <VButton
              v-permission="['system:menus:manage']"
              type="primary"
              @click="editingModal = true"
            >
              <template #icon>
                <IconAddCircle class="h-full w-full" />
              </template>
              新建
            </VButton>
          </VSpace>
        </template>
      </VEmpty>
    </Transition>
    <Transition v-else appear name="fade">
      <ul class="box-border h-full w-full divide-y divide-gray-100" role="list">
          <li v-for="friend in friends">
            <VEntity
              :is-selected="selectedFriends.includes(friend.metadata.name)"
            >
              <template #checkbox>
                <input
                  v-model="selectedFriends"
                  :value="friend.metadata.name"
                  class="h-4 w-4 rounded border-gray-300 text-indigo-600"
                  name="post-checkbox"
                  type="checkbox"
                />
              </template>

              <template #start>
                <VEntityField :title="friend.spec.displayName">
                  <template #description>
                    <a
                      :href="friend.spec.rssUrl"
                      class="truncate text-xs text-gray-500 hover:text-gray-900"
                      target="_blank"
                    >
                      {{ friend.spec.rssUrl }}
                    </a>
                  </template>
                </VEntityField>
              </template>

              <template #end>
                <VEntityField
                >
                  <template #description>
                    <a
                      :href="friend.spec.link"
                      class="truncate text-xs text-gray-500 hover:text-gray-900"
                      target="_blank"
                    >
                      {{ friend.spec.link }}
                    </a>
                  </template>
                </VEntityField>
                <VEntityField v-if="friend.metadata.deletionTimestamp">
                  <template #description>
                    <VStatusDot
                      v-tooltip="`删除中`"
                      state="warning"
                      animate
                    />
                  </template>
                </VEntityField>
                <VEntityField
                  :description="
                          formatDatetime(friend.metadata.creationTimestamp)
                        "
                />
              </template>
              <template #dropdownItems>
                <VDropdownItem @click="handleOpenCreateModal(friend)">
                  编辑
                </VDropdownItem>
                <VDropdownItem type="danger" @click="handleDelete(friend)">
                  删除
                </VDropdownItem>
              </template>
            </VEntity>
          </li>
      </ul>
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

</style>
