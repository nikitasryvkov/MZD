let currentAccessToken: string | undefined

export function setCurrentAccessToken(token?: string) {
  currentAccessToken = token
}

export function getCurrentAccessToken() {
  return currentAccessToken
}
