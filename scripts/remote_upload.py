import os
import posixpath
import sys

import paramiko


def main() -> int:
    host = os.environ.get("REMOTE_HOST")
    user = os.environ.get("REMOTE_USER")
    password = os.environ.get("REMOTE_PASS")

    if not host or not user or not password:
        print("REMOTE_HOST, REMOTE_USER, REMOTE_PASS are required.", file=sys.stderr)
        return 2

    if len(sys.argv) != 3:
        print(
            "Usage: python scripts/remote_upload.py <local_path> <remote_path>",
            file=sys.stderr,
        )
        return 2

    local_path, remote_path = sys.argv[1], sys.argv[2]

    transport = paramiko.Transport((host, 22))
    transport.connect(username=user, password=password)
    sftp = paramiko.SFTPClient.from_transport(transport)

    try:
        remote_dir = posixpath.dirname(remote_path)
        if remote_dir:
            parts = remote_dir.split("/")
            current = ""
            for part in parts:
                if not part:
                    current = "/"
                    continue
                current = posixpath.join(current, part) if current != "/" else f"/{part}"
                try:
                    sftp.stat(current)
                except FileNotFoundError:
                    sftp.mkdir(current)

        sftp.put(local_path, remote_path)
        print(f"Uploaded {local_path} -> {remote_path}")
        return 0
    finally:
        sftp.close()
        transport.close()


if __name__ == "__main__":
    raise SystemExit(main())
