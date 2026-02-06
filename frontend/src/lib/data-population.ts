import publicData from '../../public.json';
import { categoriesApi, productsApi } from '@/services/api';
import { Category, Product } from '@/types';

interface PublicProduct {
  name: string;
  price: number;
  stockQuantity: number;
  imageUrl: string;
  categoryId: number;
}

interface PublicCategory {
  id: number;
  name: string;
}

const populateData = async () => {
  try {
    // 1. Fetch existing categories from the backend
    const existingResp = await (categoriesApi as any).getAll({ size: 1000 });
    const existingCategories = (existingResp as any)?.data?.content || (existingResp as any)?.categories || [];
    const existingCategoryNames = new Set(existingCategories.map((c: any) => c.name));

    // 2. Create new categories if they don't exist
    const publicCategories: PublicCategory[] = (publicData as any)?.categories || [];
    const newCategories: Category[] = [];

    for (const publicCategory of publicCategories) {
      if (!existingCategoryNames.has(publicCategory.name)) {
        try {
          const newCategoryResp = await (categoriesApi as any).create({ name: publicCategory.name });
          const newCategory = (newCategoryResp as any)?.data || (newCategoryResp as any)?.category || newCategoryResp;
          newCategories.push(newCategory);
          console.log(`Created category: ${newCategory.name}`);
        } catch (e) {
          console.warn('Failed to create category', publicCategory.name, e);
        }
      }
    }

    // 3. Create a map of public.json categoryId to backend categoryId
    const allCategories = [...existingCategories, ...newCategories];
    const categoryIdMap = publicCategories.reduce((acc, publicCategory) => {
      const backendCategory = allCategories.find(c => c.name === publicCategory.name);
      if (backendCategory) {
        acc[publicCategory.id] = backendCategory.id;
      }
      return acc;
    }, {} as Record<number, number>);

    // 4. Create products
    const publicProducts: PublicProduct[] = (publicData as any)?.products || [];
    for (const publicProduct of publicProducts) {
      const backendCategoryId = categoryIdMap[publicProduct.categoryId];
      if (backendCategoryId) {
        const productPayload = {
          name: publicProduct.name,
          price: publicProduct.price,
          stockQuantity: publicProduct.stockQuantity,
          imageUrl: publicProduct.imageUrl,
          categoryId: backendCategoryId,
        };
        try {
          await (productsApi as any).create(productPayload);
          console.log(`Created product: ${productPayload.name}`);
        } catch (e) {
          console.warn('Failed to create product', productPayload.name, e);
        }
      } else {
        console.warn(`Could not find a matching backend category for product: ${publicProduct.name}`);
      }
    }

    alert('Data population completed successfully!');
  } catch (error) {
    console.error('Data population failed:', error);
    alert('Data population failed. Check the console for details.');
  }
};

export { populateData };
