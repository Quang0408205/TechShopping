"""ALS cho phản hồi ngầm định (Hu, Koren & Volinsky 2008; báo cáo mục 3.5.4 b)."""

import numpy as np
import pytest
from scipy.sparse import csr_matrix

from app.recommender import als


def closed_form(y: np.ndarray, r: np.ndarray, a: float, lam: float) -> np.ndarray:
    """x_u = (YᵀC_uY + λI)⁻¹ YᵀC_u p_u viết thẳng theo công thức, với C_u, p_u đầy đủ cho mọi sản phẩm."""
    c = np.diag(1 + a * r)
    p = (r > 0).astype(float)
    return np.linalg.solve(y.T @ c @ y + lam * np.eye(y.shape[1]), y.T @ c @ p)


def test_fold_in_equals_the_closed_form_of_the_report():
    rng = np.random.default_rng(1)
    y = rng.normal(size=(7, 3))
    r = np.array([4.159, 0, 0, 3.466, 0, 0.693, 0])
    rows = np.flatnonzero(r)

    x = als.fold_in(y, rows, r[rows], confidence=10, regularization=0.1)

    # cách tính nhanh YᵀY + Yᵀ(C_u − I)Y chỉ trên sản phẩm đã tương tác cho cùng kết quả với công thức đầy đủ
    assert x == pytest.approx(closed_form(y, r, 10, 0.1))


def test_training_learns_that_similar_users_like_the_same_items():
    # khách 0–3 thích sản phẩm 0–2, khách 4–7 thích 3–5; khách 3 chưa gặp sản phẩm 2
    dense = np.zeros((8, 6))
    dense[0:4, 0:3] = 1.0
    dense[4:8, 3:6] = 1.0
    dense[3, 2] = 0.0

    x, y = als.train(csr_matrix(dense), factors=4, regularization=0.1, confidence=10, iterations=15)
    scores = x[3] @ y.T

    # sản phẩm 2 chưa xem nhưng cùng "gu" được chấm cao hơn mọi sản phẩm của nhóm kia
    assert scores[2] > scores[3:6].max()


def test_training_is_reproducible_with_the_same_seed():
    matrix = csr_matrix(np.eye(4))

    first = als.train(matrix, factors=2, regularization=0.1, confidence=10, iterations=3, seed=7)
    second = als.train(matrix, factors=2, regularization=0.1, confidence=10, iterations=3, seed=7)

    assert np.array_equal(first[1], second[1])
