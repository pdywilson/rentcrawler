import pytest
from ../src/create_website import get_latest_stats

def test_get_latest_stats():
    a,b,c = get_latest_stats()
    assert a > 0
    