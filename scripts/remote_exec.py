import os
import sys

import paramiko


def main() -> int:
    host = os.environ.get("REMOTE_HOST")
    user = os.environ.get("REMOTE_USER")
    password = os.environ.get("REMOTE_PASS")

    if not host or not user or not password:
        print("REMOTE_HOST, REMOTE_USER, REMOTE_PASS are required.", file=sys.stderr)
        return 2

    if len(sys.argv) < 2:
        print("Usage: python scripts/remote_exec.py <command>", file=sys.stderr)
        return 2

    command = " ".join(sys.argv[1:])

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    try:
        client.connect(hostname=host, username=user, password=password, timeout=15)
        stdin, stdout, stderr = client.exec_command(command)
        exit_code = stdout.channel.recv_exit_status()
        out = stdout.read().decode("utf-8", errors="replace")
        err = stderr.read().decode("utf-8", errors="replace")
        if out:
            sys.stdout.write(out)
        if err:
            sys.stderr.write(err)
        return exit_code
    finally:
        client.close()


if __name__ == "__main__":
    raise SystemExit(main())
