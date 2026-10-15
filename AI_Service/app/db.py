"""Kết nối PostgreSQL. Mặc định CHỈ ĐỌC (báo cáo mục 3.5.7): mọi lệnh ghi sẽ bị PostgreSQL từ chối."""

from contextlib import contextmanager

import psycopg
from psycopg.rows import dict_row

from app.config import DATABASE_URL


@contextmanager
def connect(readonly: bool = True):
    options = "-c default_transaction_read_only=on" if readonly else ""
    with psycopg.connect(DATABASE_URL, options=options, row_factory=dict_row) as conn:
        yield conn


def fetch_all(sql: str, params=None) -> list[dict]:
    with connect() as conn:
        return conn.execute(sql, params).fetchall()
