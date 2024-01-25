import type {Friend} from "@/types";
import service from "@/api/request";
import type { Ref } from "vue";
import { onMounted, ref } from "vue";


interface useAuthorFetchReturn {
  authors: Ref<Friend[]>;
  loading: Ref<boolean>;
  handleFetchAuthors: () => void;
}

export function useAuthorFetch(options?: {
  fetchOnMounted: boolean;
}): useAuthorFetchReturn {
  const { fetchOnMounted } = options || {};

  const authors = ref<Friend[]>([] as Friend[]);
  const loading = ref(false);

  const handleFetchAuthors = async () => {
    try {
      loading.value = true;
      const { data } = await service.get(
        "/apis/api.plugin.halo.run/v1alpha1/plugins/plugin-friends/friends"
      );
      authors.value = data.items;
    } catch (e) {
      console.error("Failed to fetch friends", e);
    } finally {
      loading.value = false;
    }
  };

  onMounted(() => {
    fetchOnMounted && handleFetchAuthors();
  });

  return {
    authors,
    loading,
    handleFetchAuthors,
  };
}
