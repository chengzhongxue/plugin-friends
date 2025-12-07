import "./styles/index.css";
import { VLoading } from "@halo-dev/components";
import "uno.css";
import { definePlugin } from "@halo-dev/console-shared";
import RiBloggerLine from '~icons/ri/blogger-line';
import { defineAsyncComponent, markRaw } from "vue";
import {FriendsRssExtension} from "@/editor";
import '@kunkunyu/fridends-rss';

export default definePlugin({
  components: {},
  routes: [
    {
      parentName: "Root",
      route: {
        path: "/friends",
        name: "Friends",
        component: defineAsyncComponent({
            loader: () => import("@/views/Friend.vue"),
            loadingComponent: VLoading,
        }),
        meta: {
          title: "朋友圈",
          searchable: true,
          permissions: ["plugin:friends:view"],
          menu: {
            name: "朋友圈",
            icon: markRaw(RiBloggerLine),
            group: "content",
            priority: 20,
          },
        },
      },
    },
  ],
  extensionPoints: {
    "default:editor:extension:create": () => {
      return [FriendsRssExtension];
    },
  },
});
