const PASSWORD_ENCODING_PREFIX = 'b64:';

export function encodePassword(password: string): string {
  const normalizedPassword = password.trim();
  if (!normalizedPassword || normalizedPassword.startsWith(PASSWORD_ENCODING_PREFIX)) {
    return normalizedPassword;
  }

  if (typeof TextEncoder === 'undefined' || typeof globalThis.btoa !== 'function') {
    return normalizedPassword;
  }

  const bytes = new TextEncoder().encode(normalizedPassword);
  let binary = '';
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte);
  });

  return `${PASSWORD_ENCODING_PREFIX}${globalThis.btoa(binary)}`;
}
