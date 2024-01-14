
export interface Metadata {
  name: string;
  generateName?: string;
  labels?: {
    [key: string]: string;
  } | null;
  annotations?: {
    [key: string]: string;
  } | null;
  version?: number | null;
  creationTimestamp?: string | null;
  deletionTimestamp?: string | null;
}

export interface FriendMetadata {
  name: string;
  generateName?: string;
  labels?: {
    [key: string]: string;
  } | null;
  annotations?: {
    [key: string]: string;
  } | null;
  version?: number | null;
  creationTimestamp?: string | null;
  deletionTimestamp?: string | null;
}

export interface Friend {
  spec: FriendSpec;
  apiVersion: string;
  kind: string;
  metadata: FriendMetadata;
}


export interface FriendSpec {
  rssUrl: string;
  displayName?: string;
  logo?: string;
  link?: string;
  description?: string;
  status?:number;
  pullTime?:string;
}

export interface friendList {
  page: number;
  size: number;
  total: number;
  items: Array<Friend>;
  first: boolean;
  last: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
  totalPages: number;
}


export interface FriendPostList<T> {
  page: number;
  size: number;
  total: number;
  items: Array<T>;
  first: boolean;
  last: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
  totalPages: number;
}



export interface Spec {
  url?: string;
  author?: string;
  title?: string;
  link?: string;
  description?: string;
  pubDate?: string;
}

export interface FriendPost {
  spec: Spec;
  apiVersion: string;
  kind: string;
  metadata: Metadata;
}




