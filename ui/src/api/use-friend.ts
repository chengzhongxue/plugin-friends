import type { friendList } from "@/types";
import service from "@/api/request";
import { useQuery } from "@tanstack/vue-query";
import { ref, type Ref } from "vue";

export function useFriendFetch(
  page: Ref<number>,
  size: Ref<number>,
  keyword?: Ref<string>
) {
  const total = ref(0);

  const {
    data: friends,
    isLoading,
    refetch,
  } = useQuery({
    queryKey: ["friends", page, size,keyword],
    queryFn: async () => {
      const { data } = await service.get<friendList>(
        "/apis/api.plugin.halo.run/v1alpha1/plugins/PluginFriends/friends",
        {
          params: {
            page: page.value,
            size: size.value,
            keyword: keyword?.value
          },
        }
      );

      total.value = data.total;

      return data.items;
    },
    refetchOnWindowFocus: false,
    refetchInterval(data) {
      const deletingFriend = data?.filter(
        (friend) => !!friend.metadata.deletionTimestamp
      );
      return deletingFriend?.length ? 1000 : false;
    },
  });

  return {
    friends,
    isLoading,
    refetch,
    total,
  };
}


export function useFriendPostFetch(
  page: Ref<number>,
  size: Ref<number>,
  keyword?: Ref<string>
) {
  const total = ref(0);

  const {
    data: friendPosts,
    isLoading,
    refetch,
  } = useQuery({
    queryKey: ["friendPosts", page, size,keyword],
    queryFn: async () => {
      const { data } = await service.get<friendList>(
        "/apis/api.plugin.halo.run/v1alpha1/plugins/PluginFriends/friendPosts",
        {
          params: {
            page: page.value,
            size: size.value,
            keyword: keyword?.value
          },
        }
      );

      total.value = data.total;

      return data.items;
    },
    refetchOnWindowFocus: false,
    refetchInterval(data) {
      const deletingFriend = data?.filter(
        (friendPost) => !!friendPost.metadata.deletionTimestamp
      );
      return deletingFriend?.length ? 1000 : false;
    },
  });

  return {
    friendPosts,
    isLoading,
    refetch,
    total,
  };
}
