declare module "react-native-acr35" {
  export const read: (
    type: number
  ) => Promise<string>;

  export const sleep: () => Promise<string>;
}