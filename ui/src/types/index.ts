
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
  status: BlogStatus;
  apiVersion: string;
  kind: string;
  metadata: FriendMetadata;
}


export interface BlogStatus {

  statusType?: StatusTypeEnum;
  code?: number;
  selfSubmitted?: boolean;
}

export type StatusTypeEnum =
  (typeof StatusTypeEnum)[keyof typeof StatusTypeEnum];

declare const StatusTypeEnum: {
  readonly Ok: "OK";
  readonly Timeout: "TIMEOUT";
  readonly Can_not_be_accessed: "CAN_NOT_BE_ACCESSED";
};

export type SubmittedType =
  (typeof SubmittedType)[keyof typeof SubmittedType];

declare const SubmittedType: {
  readonly Submitted: "SUBMITTED";
  readonly System_check_valid: "SYSTEM_CHECK_VALID";
  readonly System_check_invalid: "SYSTEM_CHECK_INVALID";
  readonly Approved: "APPROVED";
  readonly Rejected: "REJECTED";
};





export interface FriendSpec {
  rssUrl: string;
  displayName?: string;
  logo?: string;
  link?: string;
  description?: string;
  status?:number;
  pullTime?:string;
  selfSubmitted?: boolean;
  submittedType?: SubmittedType;
  
  reason?: string;
}

export interface FriendList {
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


export interface FriendPostList {
  page: number;
  size: number;
  total: number;
  items: Array<FriendPost>;
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

export interface CronFriendPost {
  spec: CronFriendPostSpec;
  status?: CronFriendPostStatus
  apiVersion: string;
  kind: string;
  metadata: Metadata;
}

export interface CronFriendPostSpec {
  cron?: string;
  timezone?: string;
  suspend?: boolean;
  successfulRetainLimit?: number;
}

export interface CronFriendPostStatus {
  lastScheduledTimestamp?: number;
  nextSchedulingTimestamp?: number;
}


export interface CronFriendPostList {
  page: number;
  size: number;
  total: number;
  items: Array<CronFriendPost>;
  first: boolean;
  last: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
  totalPages: number;
}




