package de.akii.commercetools.api.customtypes.generator.model.product

import com.commercetools.api.models.cart.CartReference
import com.commercetools.api.models.cart_discount.CartDiscountReference
import com.commercetools.api.models.category.CategoryReference
import com.commercetools.api.models.channel.ChannelReference
import com.commercetools.api.models.common.LocalizedString
import com.commercetools.api.models.common.Reference
import com.commercetools.api.models.common.ReferenceTypeId
import com.commercetools.api.models.common.TypedMoney
import com.commercetools.api.models.custom_object.CustomObjectReference
import com.commercetools.api.models.customer.CustomerReference
import com.commercetools.api.models.customer_group.CustomerGroupReference
import com.commercetools.api.models.discount_code.DiscountCodeReference
import com.commercetools.api.models.inventory.InventoryEntryReference
import com.commercetools.api.models.order.OrderReference
import com.commercetools.api.models.order_edit.OrderEditReference
import com.commercetools.api.models.payment.PaymentReference
import com.commercetools.api.models.product.Attribute
import com.commercetools.api.models.product.ProductReference
import com.commercetools.api.models.product_discount.ProductDiscountReference
import com.commercetools.api.models.product_selection.ProductSelectionReference
import com.commercetools.api.models.product_type.*
import com.commercetools.api.models.review.ReviewReference
import com.commercetools.api.models.shipping_method.ShippingMethodReference
import com.commercetools.api.models.shopping_list.ShoppingListReference
import com.commercetools.api.models.state.StateReference
import com.commercetools.api.models.store.StoreReference
import com.commercetools.api.models.tax_category.TaxCategoryReference
import com.commercetools.api.models.type.TypeReference
import com.commercetools.api.models.zone.ZoneReference
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import de.akii.commercetools.api.customtypes.generator.common.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

fun productVariantAttributes(
    typedProductVariantAttributesClassName: TypedProductVariantAttributes,
    typedProductVariantAttributesInterfaceClassName: TypedProductVariantAttributesInterface,
    productType: ProductType,
    config: Configuration
): TypeSpec {
    return TypeSpec
        .classBuilder(typedProductVariantAttributesClassName.className)
        .addModifiers(
            if (productType.attributes.isEmpty())
                emptyList()
            else
                listOf(KModifier.DATA)
        )
        .addSuperinterface(typedProductVariantAttributesInterfaceClassName.className)
        .addAnnotation(generated)
        .addAnnotation(deserializeAs(typedProductVariantAttributesClassName.className))
        .primaryConstructor(constructor(productType, config))
        .addProperties(productType.attributes.map { attribute(productType, it, config) })
        .addType(productVariantAttributesCompanionObject(productType, productType.attributes, config))
        .build()
}

private fun constructor(productType: ProductType, config: Configuration): FunSpec =
    FunSpec
        .constructorBuilder()
        .addParameters(productType.attributes.map { parameter(productType, it, config) })
        .build()

private fun parameter(productType: ProductType, attributeDefinition: AttributeDefinition, config: Configuration): ParameterSpec =
    ParameterSpec
        .builder(
            config.attributeToPropertyName(productType, attributeDefinition),
            typeNameForAttributeType(attributeDefinition.type, isAttributeRequired(productType, attributeDefinition, config), config)
        )
        .addAnnotation(jsonProperty(attributeDefinition.name))
        .build()

private fun attribute(productType: ProductType, attributeDefinition: AttributeDefinition, config: Configuration): PropertySpec =
    PropertySpec
        .builder(
            config.attributeToPropertyName(productType, attributeDefinition),
            typeNameForAttributeType(attributeDefinition.type, isAttributeRequired(productType, attributeDefinition, config), config)
        )
        .initializer(config.attributeToPropertyName(productType, attributeDefinition))
        .build()

private fun typeNameForAttributeType(attributeType: AttributeType, isRequired: Boolean, config: Configuration): TypeName =
    when (attributeType) {
        is AttributeBooleanType -> Boolean::class.asTypeName()
        is AttributeTextType -> String::class.asTypeName()
        is AttributeLocalizableTextType -> LocalizedString::class.asTypeName()
        is AttributeEnumType -> AttributePlainEnumValue::class.asTypeName()
        is AttributeLocalizedEnumValue -> AttributeLocalizedEnumValue::class.asTypeName()
        is AttributeNumberType -> Double::class.asTypeName()
        is AttributeMoneyType -> TypedMoney::class.asTypeName()
        is AttributeDateType -> LocalDate::class.asTypeName()
        is AttributeTimeType -> LocalTime::class.asTypeName()
        is AttributeDateTimeType -> ZonedDateTime::class.asTypeName()
        is AttributeReferenceType -> referenceTypeIdToClassName(attributeType.referenceTypeId)
        is AttributeSetType -> SET.parameterizedBy(typeNameForAttributeType(attributeType.elementType, isRequired, config))
        is AttributeNestedType -> findProductVariantAttributesTypeByProductTypeId(attributeType.typeReference.id, config)
        else -> Any::class.asTypeName()
    }.copy(nullable = !isRequired)

private fun referenceTypeIdToClassName(referenceTypeId: ReferenceTypeId): ClassName =
    when (referenceTypeId) {
        ReferenceTypeId.CART -> CartReference::class.asClassName()
        ReferenceTypeId.CART_DISCOUNT -> CartDiscountReference::class.asClassName()
        ReferenceTypeId.CATEGORY -> CategoryReference::class.asClassName()
        ReferenceTypeId.CHANNEL -> ChannelReference::class.asClassName()
        ReferenceTypeId.CUSTOMER -> CustomerReference::class.asClassName()
        ReferenceTypeId.CUSTOMER_GROUP -> CustomerGroupReference::class.asClassName()
        ReferenceTypeId.DISCOUNT_CODE -> DiscountCodeReference::class.asClassName()
        ReferenceTypeId.INVENTORY_ENTRY -> InventoryEntryReference::class.asClassName()
        ReferenceTypeId.KEY_VALUE_DOCUMENT -> CustomObjectReference::class.asClassName()
        ReferenceTypeId.ORDER -> OrderReference::class.asClassName()
        ReferenceTypeId.ORDER_EDIT -> OrderEditReference::class.asClassName()
        ReferenceTypeId.PAYMENT -> PaymentReference::class.asClassName()
        ReferenceTypeId.PRODUCT -> ProductReference::class.asClassName()
        ReferenceTypeId.PRODUCT_DISCOUNT -> ProductDiscountReference::class.asClassName()
        ReferenceTypeId.PRODUCT_SELECTION -> ProductSelectionReference::class.asClassName()
        ReferenceTypeId.PRODUCT_TYPE -> ProductTypeReference::class.asClassName()
        ReferenceTypeId.REVIEW -> ReviewReference::class.asClassName()
        ReferenceTypeId.SHIPPING_METHOD -> ShippingMethodReference::class.asClassName()
        ReferenceTypeId.SHOPPING_LIST -> ShoppingListReference::class.asClassName()
        ReferenceTypeId.STATE -> StateReference::class.asClassName()
        ReferenceTypeId.STORE -> StoreReference::class.asClassName()
        ReferenceTypeId.TAX_CATEGORY -> TaxCategoryReference::class.asClassName()
        ReferenceTypeId.TYPE -> TypeReference::class.asClassName()
        ReferenceTypeId.ZONE -> ZoneReference::class.asClassName()
        else -> Reference::class.asClassName()
    }

private fun findProductVariantAttributesTypeByProductTypeId(productTypeId: String, config: Configuration): TypeName =
    when (val productType = config.productTypes.find { it.id == productTypeId }) {
        is ProductType -> TypedProductVariantAttributes(productType, config).className
        else -> MUTABLE_LIST.parameterizedBy(Attribute::class.asTypeName())
    }

private fun productVariantAttributesCompanionObject(productType: ProductType, attributes: List<AttributeDefinition>, config: Configuration): TypeSpec =
    TypeSpec
        .companionObjectBuilder()
        .addProperties(attributes.map {
            PropertySpec
                .builder(attributeToConstantName(productType, it, config), String::class)
                .addModifiers(KModifier.CONST)
                .initializer("%S", it.name)
                .build()
        })
        .build()

private fun isAttributeRequired(productType: ProductType, attributeDefinition: AttributeDefinition, config: Configuration): Boolean =
    config.isAttributeRequired(productType, attributeDefinition)

private fun attributeToConstantName(productType: ProductType, attributeDefinition: AttributeDefinition, config: Configuration) =
    config.attributeToPropertyName(productType, attributeDefinition)
        .split(Regex("(?=\\p{Upper})"))
        .joinToString("_") { it.uppercase() }