<script lang="ts" setup>
import { 
  VCard, 
  VEntity,
  IconCloseCircle,
  IconRefreshLine, 
  VStatusDot, 
  IconArrowLeft, 
  IconExternalLinkLine,
  IconArrowRight,
  VAvatar, 
  VEntityField,
  Dialog,
  VButton,
  VEmpty,
  VLoading,
  VPagination,
  Toast, 
  VDropdownItem,  
  IconAddCircle, 
  VSpace} from "@halo-dev/components";
import {ref, computed, watch} from "vue";
import type {Friend, FriendList} from "@/types";
import apiClient from "@/utils/api-client";
import { useQueryClient,useQuery} from "@tanstack/vue-query";
import { formatDatetime,timeAgo } from "@/utils/date";
import FriendEditingModal from "../components/FriendEditingModal.vue";
import FriendAuditModal from "../components/FriendAuditModal.vue";
import { useRouteQuery } from "@vueuse/router";


const queryClient = useQueryClient();

const selectedFriend = ref<Friend | undefined>();
const selectedFriendAudit = ref<Friend | undefined>();
const selectedFriends = ref<string[]>([]);
const checkedAll = ref(false);
const editingModal = ref(false);
const auditModal = ref(false);
const page = useRouteQuery<number>("page", 1, {
  transform: Number,
});
const size = useRouteQuery<number>("size", 20, {
  transform: Number,
});
const keyword = useRouteQuery<string>("keyword", "");
const selectedSelfSubmitted = useRouteQuery< "true" | "false"  | undefined>("selfSubmitted");
const selectedSort = useRouteQuery<string | undefined>("sort");
const selectedStatus = useRouteQuery<"OK" | "TIMEOUT" | "CAN_NOT_BE_ACCESSED" | undefined>("status");
const selectedSubmittedType = useRouteQuery<
  "SUBMITTED" | "SYSTEM_CHECK_VALID" | "SYSTEM_CHECK_INVALID" | "APPROVED" | "REJECTED" | undefined
>("submittedType");
const hasPrevious = ref(false);
const hasNext = ref(false);
const total = ref(0);
const searchText = ref("");

watch(
  () => [
    selectedSelfSubmitted.value,
    selectedSort.value,
    selectedStatus.value,
    selectedSubmittedType.value,
    keyword.value,
  ],
  () => {
    page.value = 1;
  }
);

function handleClearFilters() {
  selectedSubmittedType.value = undefined;
  selectedStatus.value = undefined;
  selectedSort.value = undefined;
  selectedSelfSubmitted.value = undefined;
}

const hasFilters = computed(() => {
  return (
    selectedSubmittedType.value ||
    selectedStatus.value !== undefined ||
    selectedSort.value ||
    selectedSelfSubmitted.value
  );
});

const {
  data: friends,
  isLoading,
  isFetching,
  refetch,
} = useQuery({
  queryKey: ["friends", page, size,selectedStatus,selectedSelfSubmitted,selectedSubmittedType,selectedSort,keyword],
  queryFn: async () => {
    const { data } = await apiClient.get<FriendList>(
      "/apis/api.plugin.halo.run/v1alpha1/plugins/plugin-friends/friends",
      {
        params: {
          page: page.value,
          size: size.value,
          statusType: selectedStatus.value,
          selfSubmitted: selectedSelfSubmitted.value,
          submittedType: selectedSubmittedType.value,
          sort: selectedSort.value,
          keyword: keyword?.value
        },
      }
    );
    total.value = data.total;
    hasNext.value = data.hasNext;
    hasPrevious.value = data.hasPrevious;

    return data.items;
  },
  refetchInterval: (data) =>  {
    const deletingFriend = data?.filter(
      (friend) => !!friend.metadata.deletionTimestamp
    );
    return deletingFriend?.length ? 1000 : false;
  },
});

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
        await apiClient.delete(
          `/apis/api.plugin.halo.run/v1alpha1/plugins/plugin-friends/friendPost/delByLink/${friend.metadata.name}`
        )
        await apiClient.delete(
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
    title: "是否确认删除所选的订阅链接？",
    description: "删除之后将无法恢复。",
    confirmType: "danger",
    onConfirm: async () => {
      try {

        const promises = selectedFriends.value.map((friend) => {
          apiClient.delete(
            `/apis/api.plugin.halo.run/v1alpha1/plugins/plugin-friends/friendPost/delByLink/${friend}`
          )
          return apiClient.delete(`/apis/friend.moony.la/v1alpha1/friends/${friend}`);
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

const onAuditModalClose = async () => {
  selectedFriendAudit.value = undefined;
  refetch();
};

const handleSelectPrevious = () => {
  if (!friends.value) {
    return;
  }

  const index = friends.value.findIndex(
    (friend) => friend.metadata.name === selectedFriend.value?.metadata.name
  );

  if (index > 0) {
    selectedFriend.value = friends.value[index - 1];
    return;
  }
  if (index <= 0) {
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
const handleOpenAuditModal = (friend: Friend) => {
  selectedFriendAudit.value = friend;
  auditModal.value = true;
};

const getStatusType = (friend: Friend) => {
  const { status } = friend;
  return status.statusType == "OK" ? "success" : "warning";
};

const getStatusTypeText = (friend: Friend) => {
  const { status } = friend;
  return status.statusType == 'OK' ? "运行良好" :  status.statusType == 'TIMEOUT' ? '超时' : '不能访问';
};


const getState = (friend: Friend) => {
  const { spec } = friend;
  return spec.status == 1 ? "success" : "warning";
};

const getStateText = (friend: Friend) => {
  const { spec } = friend;
  return spec.status == 1 ? "同步成功" : "同步失败";
};
function handleReset() {
  keyword.value = "";
  searchText.value = "";
}
function onKeywordChange() {
  keyword.value = searchText.value;
}
</script>

<template>
  <FriendAuditModal
    v-model:visible="auditModal"
    :friend="selectedFriendAudit"
    @close="onAuditModalClose"
  >
  </FriendAuditModal>
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
      <div class="block w-full bg-gray-50 px-4 py-3">
        <div class="relative flex flex-col flex-wrap items-start gap-4 sm:flex-row sm:items-center">
          <div class="hidden items-center sm:flex">
            <input
              v-model="checkedAll"
              type="checkbox"
              @change="handleCheckAllChange"
            />
          </div>
          <div class="flex w-full flex-1 items-center sm:w-auto">
            <FormKit
              v-if="!selectedFriends.length"
              v-model="searchText"
              placeholder="输入关键词搜索"
              type="text"
              outer-class="!moments-p-0 moments-mr-2"
              @keyup.enter="onKeywordChange"
            >
              <template v-if="keyword" #suffix>
                <div
                  class="group flex h-full cursor-pointer items-center bg-white px-2 transition-all hover:bg-gray-50"
                  @click="handleReset"
                >
                  <IconCloseCircle
                    class="h-4 w-4 text-gray-500 group-hover:text-gray-700"
                  />
                </div>
              </template>
            </FormKit>
            <VSpace v-else v-permission="['plugin:friends:manage']">
              <VButton type="danger" @click="handleDeleteInBatch">
                删除
              </VButton>
            </VSpace>
          </div>
          <VSpace spacing="lg" class="flex-wrap">
              <FilterCleanButton
                v-if="hasFilters"
                @click="handleClearFilters"
              />
              <FilterDropdown
                v-model="selectedSelfSubmitted"
                label="提交类型"
                :items="[
                    {
                      label: '全部',
                      value: undefined,
                    },
                    {
                      label: '自行提交',
                      value: 'true',
                    },
                    {
                      label: '后台收录',
                      value: 'false',
                    },
                  ]"
              />
              <FilterDropdown
                v-model="selectedSubmittedType"
                label="审核状态"
                :items="[
                    {
                      label: '全部',
                      value: undefined,
                    },
                    {
                      label: '提交',
                      value: 'SUBMITTED',
                    },
                    {
                      label: '系统检查有效',
                      value: 'SYSTEM_CHECK_VALID',
                    },
                    {
                      label: '系统检查无效',
                      value: 'SYSTEM_CHECK_INVALID',
                    },
                    {
                      label: '批准',
                      value: 'APPROVED',
                    },
                    {
                      label: '驳回',
                      value: 'REJECTED',
                    },
                    
                  ]"
              />
              <FilterDropdown
                v-model="selectedStatus"
                label="在线状态"
                :items="[
                      {
                        label: '全部',
                        value: undefined,
                      },
                      {
                        label: '在线',
                        value: 'OK',
                      },
                      {
                        label: '超时',
                        value: 'TIMEOUT',
                      },
                      {
                        label: '不能访问',
                        value: 'CAN_NOT_BE_ACCESSED',
                      },
                    ]"
              />
              <FilterDropdown
                v-model="selectedSort"
                label="排序"
                :items="[
                    {
                      label: '默认',
                    },
                    {
                      label: '较近更新',
                      value: 'updateTime,desc',
                    },
                    {
                      label: '较早更新',
                      value: 'updateTime,asc',
                    },
                    {
                      label: '较近创建',
                      value: 'creationTimestamp,desc',
                    },
                    {
                      label: '较早创建',
                      value: 'creationTimestamp,asc',
                    },
                  ]"
              />
              <div class="flex flex-row gap-2">
                <div
                  class="group cursor-pointer rounded p-1 hover:bg-gray-200"
                  @click="refetch()"
                >
                  <IconRefreshLine
                    v-tooltip="'刷新'"
                    :class="{ 'animate-spin text-gray-900': isFetching }"
                    class="h-4 w-4 text-gray-600 group-hover:text-gray-900"
                  />
                </div>
              </div>
          </VSpace>
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
                <VEntityField>
                  <template #description>
                    <VAvatar
                      :key="friend.metadata.name"
                      :alt="friend.spec.displayName"
                      :src="friend.spec.logo"
                      size="md"
                    ></VAvatar>
                  </template>
                </VEntityField>

                <VEntityField  :title="friend.spec.displayName">
                  <template #extra>
                    <VSpace class="mt-1 sm:mt-0">
                      <a
                        v-if="friend.spec.link"
                        target="_blank"
                        :href="friend.spec.link"
                        :title="friend.spec.link"
                        class="hidden text-gray-600 transition-all hover:text-gray-900 group-hover:inline-block"
                      >
                        <IconExternalLinkLine class="h-3.5 w-3.5" />
                      </a>
                    </VSpace>
                  </template>
                  <template #description>
                    <VSpace>
                      <span class="text-xs text-gray-500">
                       {{ friend.spec.selfSubmitted ? '自行提交' : '后台收录' }}
                      </span>
                      <p
                        class="inline-flex flex-wrap gap-1 text-xs text-gray-500"
                      >
                        rss：<a
                        :href="friend.spec.rssUrl"
                        :title="friend.spec.rssUrl"
                        target="_blank"
                        class="cursor-pointer hover:text-gray-900"
                      >
                        {{ friend.spec.rssUrl }}
                      </a>
                      </p>
                    </VSpace>
                  </template>
                </VEntityField>
              </template>

              <template #end>
                <VEntityField v-if="friend.spec.submittedType !=null && friend.spec.submittedType != ''"
                              v-tooltip="friend.spec.reason">
                  <template #description>
                    <span v-if="friend.spec.submittedType == 'SUBMITTED'" 
                          class="entity-field-description" title="提交">提交</span>
                    <span v-if="friend.spec.submittedType == 'SYSTEM_CHECK_VALID'"
                          class="entity-field-description" title="系统检查有效">系统检查有效</span>
                    <span v-if="friend.spec.submittedType == 'SYSTEM_CHECK_INVALID'"
                          class="entity-field-description" title="系统检查无效">系统检查无效</span>
                    <span v-if="friend.spec.submittedType == 'APPROVED'"
                          class="entity-field-description" title="批准">批准</span>
                    <span v-if="friend.spec.submittedType == 'REJECTED'"
                          class="entity-field-description" title="拒绝了">拒绝了</span>
                  </template>
                </VEntityField>
                <VEntityField v-if="friend.status?.statusType !=null && friend.status?.statusType != ''" :description="getStatusTypeText(friend)">
                  <template #description>
                    <VStatusDot :state="getStatusType(friend)" :text="getStatusTypeText(friend)" />
                  </template>
                </VEntityField>
                <VEntityField v-if="friend.spec.status !=null && friend.spec.status != ''" :description="getStateText(friend)">
                  <template #description>
                    <VStatusDot :state="getState(friend)" :text="getStateText(friend)" />
                  </template>
                </VEntityField>
                <VEntityField
                  v-if="friend.spec.pullTime !=null && friend.spec.pullTime != ''"
                  v-tooltip="formatDatetime(friend.spec.pullTime)"
                  :description="'同步时间：'+timeAgo(friend.spec.pullTime)"
                ></VEntityField>
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
              <template #dropdownItems >
                <VDropdownItem v-if="friend.spec.submittedType != 'SYSTEM_CHECK_VALID' && friend.spec.submittedType != 'APPROVED'" 
                               v-permission="['plugin:friends:manage']" @click="handleOpenAuditModal(friend)">
                  审核
                </VDropdownItem>
                <VDropdownItem v-permission="['plugin:friends:manage']" @click="handleOpenCreateModal(friend)">
                  编辑
                </VDropdownItem>
                <VDropdownItem v-permission="['plugin:friends:manage']" type="danger" @click="handleDelete(friend)">
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

<style lang="scss">

.entity-field-wrapper {
  max-width: 25rem;
}

li.formkit-option[data-disabled="true"]{
  opacity: 0.5;
}

</style>
