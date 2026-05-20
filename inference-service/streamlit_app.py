import json
import os

import plotly.graph_objects as go
import requests
import streamlit as st

API_URL = os.environ.get("INFERENCE_API_URL", "http://localhost:8081")

MATCH_COLORS = {
    "match": "#22c55e",
    "weak_match": "#eab308",
    "related": "#f97316",
    "no_match": "#ef4444",
}

MATCH_LABELS = {
    "match": "Match",
    "weak_match": "Weak Match",
    "related": "Related",
    "no_match": "No Match",
}

EXAMPLE_SCENARIOS = {
    "-- Select an example --": None,
    "Leather wallets vs leather artisan (match)": {
        "rfq_title": "High-End Leather Card Holders / Wallets",
        "rfq_description": "We are looking for a new manufacturing partner to relaunch two existing card holder / wallet models. Products: Slim card holder / wallet (2 models). Quantities: Small series, approx. 20-100 pcs per model per year. Materials: Leather will be supplied by us (box calf and exotic leathers). Quality level: Premium / luxury quality.",
        "delivery_location": "FR",
        "quantity": "20-100 pieces per model per year",
        "category_id": 100033,
        "rfq_supplier_types": "PRODUCTION",
        "supplier_name": "GUARNICIONERIA HNOS. PEDRAZA",
        "supplier_country": "ES",
        "distribution_area": "",
        "supplier_description": "WE ARE LEATHER ARTISANS, MANUFACTURING ALL TYPES OF ITEMS FOR HUNTING PRACTICE. WE USE FIRST-CLASS LEATHERS SUCH AS CALF HIDES FOR THE PRODUCTION OF RIFLE AND SHOTGUN CASES, GREASED SUEDE FOR HOLSTERS AND CASES, AND ALSO NUBUCK AND BOX-CALF TO CREATE LADIES BAGS AND BELTS.",
        "supplier_types": "Production",
        "products": "",
        "product_categories": "Textile production",
        "keywords": "",
    },
    "Polo shirts vs sportswear (weak_match)": {
        "rfq_title": "Custom embroidered polo shirts corporate",
        "rfq_description": "We need 500 custom embroidered polo shirts with company logo for our sales team. 100% cotton piqué, various sizes S-XXL.",
        "delivery_location": "DE",
        "quantity": "500",
        "category_id": 100033,
        "rfq_supplier_types": "MANUFACTURER",
        "supplier_name": "Sporttextil Berger KG",
        "supplier_country": "CN",
        "distribution_area": "international",
        "supplier_description": "Manufacturer of sportswear and activewear. We produce running shirts, cycling jerseys, and gym wear using polyester and elastane. We do not work with cotton.",
        "supplier_types": "Wholesaler",
        "products": "Running shirts,Cycling jerseys,Gym leggings,Sports bras",
        "product_categories": "Textile production",
        "keywords": "sportswear,running,cycling,polyester,activewear",
    },
    "Herbal tea vs coffee roaster (related)": {
        "rfq_title": "Organic herbal tea blends private label",
        "rfq_description": "We are seeking a tea manufacturer for private label organic herbal tea blends. Chamomile, peppermint, rooibos blends. Packaged in biodegradable tea bags, retail-ready boxes. EU organic certification required.",
        "delivery_location": "DE",
        "quantity": "20000 boxes",
        "category_id": 100010,
        "rfq_supplier_types": "MANUFACTURER",
        "supplier_name": "Kafferösterei Bremen GmbH",
        "supplier_country": "DE",
        "distribution_area": "europe",
        "supplier_description": "Specialty coffee roaster offering single-origin and blended coffees. We roast, grind, and package coffee beans for wholesale and private label. We do not process tea or herbal products.",
        "supplier_types": "Production",
        "products": "Single-origin coffee beans,Espresso blends,Ground coffee,Coffee capsules",
        "product_categories": "Food and beverages",
        "keywords": "Kaffee,Rösterei,Espresso,Bohnen,Kaffeeröstung,Bio-Kaffee,Privatmarke",
    },
    "Concrete trucks vs yoga studio (no_match)": {
        "rfq_title": "Concrete mixer trucks 8 cubic meters",
        "rfq_description": "We need concrete mixer trucks with 8m3 drum capacity for our construction fleet. Must be Euro 6 emission standard.",
        "delivery_location": "PL",
        "quantity": "10",
        "category_id": 100080,
        "rfq_supplier_types": "PRODUCTION",
        "supplier_name": "Sunshine Yoga Studio",
        "supplier_country": "TH",
        "distribution_area": "",
        "supplier_description": "",
        "supplier_types": "Service provider",
        "products": "",
        "product_categories": "Sports and fitness",
        "keywords": "",
    },
}


def build_gauge(match_type: str, probability: float) -> go.Figure:
    color = MATCH_COLORS[match_type]
    fig = go.Figure(
        go.Indicator(
            mode="gauge+number",
            value=probability * 100,
            number={"suffix": "%", "font": {"size": 40}},
            gauge={
                "axis": {"range": [0, 100], "tickwidth": 1},
                "bar": {"color": color},
                "bgcolor": "#f8fafc",
                "borderwidth": 0,
                "steps": [
                    {"range": [0, 25], "color": "#fee2e2"},
                    {"range": [25, 50], "color": "#fef9c3"},
                    {"range": [50, 75], "color": "#fef3c7"},
                    {"range": [75, 100], "color": "#dcfce7"},
                ],
            },
        )
    )
    fig.update_layout(
        height=220,
        margin={"l": 30, "r": 30, "t": 30, "b": 10},
        paper_bgcolor="rgba(0,0,0,0)",
        plot_bgcolor="rgba(0,0,0,0)",
    )
    return fig


def build_bar_chart(probabilities: dict) -> go.Figure:
    labels = [MATCH_LABELS[k] for k in probabilities]
    values = [v * 100 for v in probabilities.values()]
    colors = [MATCH_COLORS[k] for k in probabilities]

    fig = go.Figure(
        go.Bar(
            x=values,
            y=labels,
            orientation="h",
            marker_color=colors,
            text=[f"{v:.1f}%" for v in values],
            textposition="outside",
            textfont={"size": 14},
        )
    )
    fig.update_layout(
        height=200,
        margin={"l": 10, "r": 60, "t": 10, "b": 10},
        xaxis={"range": [0, 105], "showgrid": False, "showticklabels": False},
        yaxis={"autorange": "reversed"},
        paper_bgcolor="rgba(0,0,0,0)",
        plot_bgcolor="rgba(0,0,0,0)",
    )
    return fig


# ── Page config ──────────────────────────────────────────────
st.set_page_config(
    page_title="RFQ–Supplier Match Classifier",
    page_icon="🔍",
    layout="wide",
)

st.markdown(
    """
    <style>
    .main .block-container { max-width: 1100px; padding-top: 2rem; }
    div[data-testid="stVerticalBlock"] > div { gap: 0.5rem; }
    .match-badge {
        display: inline-block; padding: 6px 18px; border-radius: 20px;
        font-weight: 700; font-size: 1.3rem; letter-spacing: 0.5px;
    }
    </style>
    """,
    unsafe_allow_html=True,
)

# ── Header ───────────────────────────────────────────────────
st.title("🔍 RFQ–Supplier Match Classifier")
st.caption(
    "Predict how well a supplier matches a Request for Quotation "
    "using a fine-tuned XLM-RoBERTa model."
)

# ── Example picker ───────────────────────────────────────────
selected_example = st.selectbox(
    "Load an example scenario",
    list(EXAMPLE_SCENARIOS.keys()),
    index=0,
)
example = EXAMPLE_SCENARIOS[selected_example]


def _val(field: str, fallback: str = "") -> str:
    if example is not None:
        return str(example.get(field, fallback))
    return fallback


def _int_val(field: str, fallback=None):
    if example is not None:
        return example.get(field, fallback)
    return fallback


st.divider()

# ── Input form ───────────────────────────────────────────────
col_rfq, col_gap, col_sup = st.columns([10, 1, 10])

with col_rfq:
    st.subheader("📋 Request for Quotation")
    rfq_title = st.text_input("Title", value=_val("rfq_title"), key="rfq_title")
    rfq_description = st.text_area(
        "Description", value=_val("rfq_description"), height=120, key="rfq_desc"
    )
    c1, c2 = st.columns(2)
    delivery_location = c1.text_input(
        "Delivery location", value=_val("delivery_location"), key="del_loc"
    )
    quantity = c2.text_input("Quantity", value=_val("quantity"), key="qty")
    c3, c4 = st.columns(2)
    category_id = c3.number_input(
        "Category ID",
        value=_int_val("category_id", 100033),
        min_value=0,
        step=1,
        key="cat_id",
    )
    rfq_supplier_types = c4.text_input(
        "Wanted supplier types",
        value=_val("rfq_supplier_types"),
        key="rfq_sup_types",
    )

with col_sup:
    st.subheader("🏭 Supplier")
    supplier_name = st.text_input(
        "Company name", value=_val("supplier_name"), key="sup_name"
    )
    supplier_description = st.text_area(
        "Description",
        value=_val("supplier_description"),
        height=120,
        key="sup_desc",
    )
    c5, c6 = st.columns(2)
    supplier_country = c5.text_input(
        "Country", value=_val("supplier_country"), key="sup_country"
    )
    distribution_area = c6.text_input(
        "Distribution area", value=_val("distribution_area"), key="dist_area"
    )
    c7, c8 = st.columns(2)
    supplier_types = c7.text_input(
        "Supplier type", value=_val("supplier_types"), key="sup_type"
    )
    product_categories = c8.text_input(
        "Product categories",
        value=_val("product_categories"),
        key="prod_cats",
    )
    products = st.text_input("Products", value=_val("products"), key="prods")
    keywords = st.text_input("Keywords", value=_val("keywords"), key="kw")

st.divider()

# ── Predict ──────────────────────────────────────────────────
_, btn_col, _ = st.columns([4, 3, 4])
predict_clicked = btn_col.button(
    "🚀  Classify Match", use_container_width=True, type="primary"
)

if predict_clicked:
    payload = {
        "rfq_title": rfq_title,
        "rfq_description": rfq_description,
        "delivery_location": delivery_location,
        "quantity": quantity,
        "category_id": int(category_id) if category_id else None,
        "rfq_supplier_types": rfq_supplier_types,
        "supplier_name": supplier_name,
        "supplier_country": supplier_country,
        "distribution_area": distribution_area,
        "supplier_description": supplier_description,
        "supplier_types": supplier_types,
        "products": products,
        "product_categories": product_categories,
        "keywords": keywords,
    }

    try:
        with st.spinner("Running inference..."):
            resp = requests.post(f"{API_URL}/predict", json=payload, timeout=30)
            resp.raise_for_status()
            result = resp.json()
    except requests.ConnectionError:
        st.error(
            "Cannot reach the FastAPI backend at "
            f"**{API_URL}**. Make sure it is running."
        )
        st.stop()
    except Exception as exc:
        st.error(f"Request failed: {exc}")
        st.stop()

    match_type = result["match_type"]
    probs = result["probabilities"]
    top_prob = probs[match_type]
    color = MATCH_COLORS[match_type]

    st.divider()

    # ── Result header ────────────────────────────────────────
    res_left, res_right = st.columns([5, 6])

    with res_left:
        st.markdown("#### Prediction")
        st.markdown(
            f'<span class="match-badge" style="background:{color}22;color:{color};">'
            f"{MATCH_LABELS[match_type]}</span>",
            unsafe_allow_html=True,
        )
        st.plotly_chart(build_gauge(match_type, top_prob), use_container_width=True)

    with res_right:
        st.markdown("#### Class Probabilities")
        st.plotly_chart(build_bar_chart(probs), use_container_width=True)

    # ── Raw JSON ─────────────────────────────────────────────
    with st.expander("Raw API response"):
        st.code(json.dumps(result, indent=2), language="json")
