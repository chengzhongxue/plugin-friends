import type { AxiosInstance } from "axios";
import axios from "axios";

const service: AxiosInstance = axios.create({
  baseURL: "/",
  timeout: 5000,
});

export default service;
