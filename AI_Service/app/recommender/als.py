"""
Collaborative Filtering bằng ALS cho phản hồi ngầm định (báo cáo mục 3.5.4 b; Hu, Koren & Volinsky 2008).

    p_ui = 1 nếu r(u,i) > 0, ngược lại 0;      c_ui = 1 + a·r(u,i)
    min Σ c_ui (p_ui − x_uᵀ y_i)² + λ (Σ‖x_u‖² + Σ‖y_i‖²)

Cố định Y thì mỗi x_u có nghiệm đóng:  x_u = (YᵀY + Yᵀ(C_u − I)Y + λI)⁻¹ Yᵀ C_u p_u
(chỉ cần duyệt các sản phẩm u đã tương tác vì YᵀY tính một lần), rồi làm tương tự cho Y, luân phiên.
"""

import numpy as np
from scipy.sparse import csr_matrix


def _solve_rows(fixed: np.ndarray, matrix: csr_matrix, confidence: float, regularization: float) -> np.ndarray:
    factors = fixed.shape[1]
    gram = fixed.T @ fixed
    identity = regularization * np.eye(factors)
    solved = np.zeros((matrix.shape[0], factors))
    for row in range(matrix.shape[0]):
        start, end = matrix.indptr[row], matrix.indptr[row + 1]
        if start == end:
            continue
        cols = matrix.indices[start:end]
        conf = 1.0 + confidence * matrix.data[start:end]
        y = fixed[cols]
        a = gram + (y.T * (conf - 1.0)) @ y + identity
        b = (y * conf[:, None]).sum(axis=0)
        solved[row] = np.linalg.solve(a, b)
    return solved


def train(user_item: csr_matrix, factors: int, regularization: float, confidence: float, iterations: int,
          seed: int = 42) -> tuple[np.ndarray, np.ndarray]:
    """user_item[u, i] = r(u,i). Trả về (X: vector ẩn người dùng, Y: vector ẩn sản phẩm)."""
    rng = np.random.default_rng(seed)
    n_users, n_items = user_item.shape
    x = rng.normal(0, 0.01, (n_users, factors))
    y = rng.normal(0, 0.01, (n_items, factors))
    item_user = user_item.T.tocsr()
    for _ in range(iterations):
        x = _solve_rows(y, user_item, confidence, regularization)
        y = _solve_rows(x, item_user, confidence, regularization)
    return x, y


def fold_in(y: np.ndarray, item_indices: np.ndarray, scores: np.ndarray, confidence: float,
            regularization: float) -> np.ndarray:
    """
    Vector x_u cho người dùng từ các tương tác HIỆN TẠI với Y đã huấn luyện (một bước ALS),
    nên hành vi mới sau lần huấn luyện gần nhất vẫn được tính mà không cần huấn luyện lại.
    """
    row = csr_matrix((scores, (np.zeros_like(item_indices), item_indices)), shape=(1, y.shape[0]))
    return _solve_rows(y, row, confidence, regularization)[0]
